"""Validate the instrumentation APK paired with the original-key app delivery."""
import argparse
import re
from verify_release_apk import PACKAGE, SIGNER
from apk_manifest import parse_xmltree, read_apk, verify_instrumentation, verify_signer


def validate_test(badging, certificates, xmltree):
    package = re.search(r"^package: name='([^']+)'", badging, re.M)
    if not package or package.group(1) != PACKAGE + ".test":
        raise ValueError("Unexpected instrumentation APK package")
    manifest = parse_xmltree(xmltree)
    if manifest.attributes.get("package") != PACKAGE + ".test":
        raise ValueError("Instrumentation package differs from badging")
    verify_instrumentation(manifest, "androidx.test.runner.AndroidJUnitRunner", PACKAGE)
    verify_signer(certificates, SIGNER)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--test-apk", required=True)
    parser.add_argument("--build-tools", required=True)
    args = parser.parse_args()
    validate_test(*read_apk(args.test_apk, args.build_tools))
    print("Verified original-key instrumentation package and production target")


if __name__ == "__main__":
    main()
