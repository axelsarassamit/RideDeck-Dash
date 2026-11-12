"""Validate public release identity after packaging, before any upload or publication."""
import argparse
import os
from pathlib import Path
import re
import subprocess

PACKAGE = "com.axelsarassamit.ridedeck.dash"
SIGNER = "cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b"


def validate(tag, badging, certificates):
    if not re.fullmatch(r"v[0-9]{1,3}\.[0-9]{1,2}\.[0-9]{1,2}", tag):
        raise ValueError("Release tag must be numeric vMAJOR.MINOR.PATCH")
    version = tag[1:]
    major, minor, patch = map(int, version.split("."))
    code = 100000 + major * 10000 + minor * 100 + patch
    match = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging, re.M)
    if not match or match.groups() != (PACKAGE, str(code), version):
        raise ValueError("APK package, versionCode or versionName does not match the release tag")
    if re.search(r"^application-debuggable(?:\s|$)", badging, re.M):
        raise ValueError("A debuggable APK must not be published as a release")
    signers = re.findall(r"^Signer #[0-9]+ certificate SHA-256 digest: ([0-9a-fA-F]+)$", certificates, re.M)
    if [signer.lower() for signer in signers] != [SIGNER]:
        raise ValueError("APK does not use the permanent Dash release signer")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--build-tools", required=True)
    args = parser.parse_args()
    tools = Path(args.build_tools)
    badging = subprocess.check_output([str(tools / ("aapt.exe" if os.name == "nt" else "aapt")), "dump", "badging", args.apk], text=True)
    certificates = subprocess.check_output([str(tools / ("apksigner.bat" if os.name == "nt" else "apksigner")), "verify", "--print-certs", args.apk], text=True)
    validate(args.tag, badging, certificates)
    print("Verified Dash release package, version, non-debuggable build and permanent signer")


if __name__ == "__main__":
    main()
