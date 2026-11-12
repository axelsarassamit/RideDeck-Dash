import copy
import sys
from pathlib import Path
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from validate_original_delivery import REPOSITORY, validate_ci
from verify_original_delivery import validate_test
from verify_release_apk import PACKAGE, SIGNER

SHA = "a" * 40
FIXTURES = Path(__file__).parent / "fixtures"

def successful_run():
    return {"id": 123, "head_sha": SHA, "status": "completed", "conclusion": "success", "name": "Android build", "path": ".github/workflows/android.yml", "head_branch": "main", "event": "push", "repository": {"full_name": REPOSITORY}, "head_repository": {"full_name": REPOSITORY}}

class OriginalDeliveryTest(unittest.TestCase):
    def test_only_successful_exact_android_ci_is_accepted(self):
        self.assertEqual(SHA, validate_ci(SHA.upper(), 123, successful_run()))
        for field, value in [("id", 124), ("head_sha", "b" * 40), ("status", "in_progress"), ("conclusion", "failure"), ("name", "Signed release"), ("path", ".github/workflows/release.yml"), ("head_branch", "other"), ("event", "pull_request")]:
            run = successful_run(); run[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError): validate_ci(SHA, 123, run)

    def test_unknown_fork_and_injected_refs_fail(self):
        for field in ("repository", "head_repository"):
            run = successful_run(); run[field] = {"full_name": "other/repo"}
            with self.assertRaises(ValueError): validate_ci(SHA, 123, run)
        for sha in ("main", "a" * 39, "a" * 41, ";echo secret"):
            with self.assertRaises(ValueError): validate_ci(sha, 123, successful_run())
        for run_id in (0, -1, "123?other=1"):
            with self.assertRaises(ValueError): validate_ci(SHA, run_id, successful_run())
        with self.assertRaises(ValueError): validate_ci(SHA, 123, successful_run(), "other/repo")

    def test_instrumentation_identity(self):
        badging = (FIXTURES / "dash-release-tests-badging.txt").read_text(encoding="utf-8")
        xmltree = (FIXTURES / "dash-release-tests-xmltree.txt").read_text(encoding="utf-8")
        self.assertNotIn("instrumentation:", badging)
        cert = "Signer #1 certificate SHA-256 digest: " + SIGNER
        validate_test(badging, cert, xmltree)
        for invalid, certificates, tree in [
            (badging.replace(PACKAGE + ".test", PACKAGE + ".debug.test"), cert, xmltree),
            (badging, cert, xmltree.replace('"' + PACKAGE + '"', '"other"')),
            (badging, "Signer #1 certificate SHA-256 digest: bad", xmltree),
            (badging, cert, xmltree.replace("AndroidJUnitRunner", "OtherRunner")),
        ]:
            with self.assertRaises(ValueError): validate_test(invalid, certificates, tree)

if __name__ == "__main__": unittest.main()
