"""Read-only prerequisite check for manually authorized original-key delivery."""
import argparse
import json
import os
import re
import subprocess

REPOSITORY = "axelsarassamit/RideDeck-Dash"


def validate_ci(source_sha, run_id, run, repository=REPOSITORY):
    if repository != REPOSITORY:
        raise ValueError("Delivery is restricted to the owning Dash repository")
    if not re.fullmatch(r"[0-9a-fA-F]{40}", source_sha):
        raise ValueError("source_sha must be an exact 40-character commit SHA")
    if not re.fullmatch(r"[1-9][0-9]*", str(run_id)):
        raise ValueError("ci_run_id must identify the manually checked Android build")
    sha = source_sha.lower()
    if str(run.get("id")) != str(run_id) or run.get("head_sha") != sha:
        raise ValueError("CI does not match the requested commit and run")
    if run.get("status") != "completed" or run.get("conclusion") != "success":
        raise ValueError("Exact Android CI must have completed successfully")
    if run.get("name") != "Android build" or run.get("path") != ".github/workflows/android.yml":
        raise ValueError("The prerequisite must be the Android build workflow")
    if run.get("head_branch") != "main" or run.get("event") not in ("push", "workflow_dispatch"):
        raise ValueError("Delivery requires trusted main-branch Android CI")
    for key in ("repository", "head_repository"):
        if (run.get(key) or {}).get("full_name") != REPOSITORY:
            raise ValueError("Fork or unknown repository CI is not accepted")
    return sha


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-sha", required=True)
    parser.add_argument("--ci-run-id", required=True)
    args = parser.parse_args()
    # Validate syntax before constructing an API endpoint or checkout ref.
    if not re.fullmatch(r"[0-9a-fA-F]{40}", args.source_sha) or not re.fullmatch(r"[1-9][0-9]*", args.ci_run_id):
        raise ValueError("Invalid source SHA or CI run ID")
    if os.environ.get("GITHUB_REF") != "refs/heads/main":
        raise ValueError("Dispatch this workflow only from main")
    run = json.loads(subprocess.check_output(["gh", "api", f"repos/{REPOSITORY}/actions/runs/{args.ci_run_id}"], text=True))
    sha = validate_ci(args.source_sha, args.ci_run_id, run, os.environ.get("GITHUB_REPOSITORY"))
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write("source_sha=" + sha + "\n")
    print("Verified successful exact main-branch Android CI for " + sha)


if __name__ == "__main__":
    main()
