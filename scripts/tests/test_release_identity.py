import sys
from pathlib import Path
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_release_apk import validate, SIGNER

class ReleaseIdentityTest(unittest.TestCase):
    def test_valid_release(self):
        validate("v0.13.7", "package: name='com.axelsarassamit.ridedeck.dash' versionCode='101307' versionName='0.13.7'", "Signer #1 certificate SHA-256 digest: " + SIGNER)

    def test_future_production_one_point_zero(self):
        validate("v1.0.0", "package: name='com.axelsarassamit.ridedeck.dash' versionCode='110000' versionName='1.0.0'", "Signer #1 certificate SHA-256 digest: " + SIGNER)

    def test_actual_mislabeled_138_asset_is_rejected(self):
        with self.assertRaises(ValueError):
            validate("v1.3.8", "package: name='com.axelsarassamit.ridedeck.dash' versionCode='10305' versionName='1.3.5-dev'", "Signer #1 certificate SHA-256 digest: ce1eaec35ba8ba0a17505953ddb880c7ca87ce4ad63c88d8ded1c263ccb0d2df")

    def test_wrong_signer_debug_and_wrong_package_fail_closed(self):
        good = "package: name='com.axelsarassamit.ridedeck.dash' versionCode='101307' versionName='0.13.7'"
        for badging, cert in [(good, "Signer #1 certificate SHA-256 digest: bad"), (good + "\napplication-debuggable", "Signer #1 certificate SHA-256 digest: " + SIGNER), (good.replace("dash'", "dash.debug'"), "Signer #1 certificate SHA-256 digest: " + SIGNER)]:
            with self.assertRaises(ValueError):
                validate("v0.13.7", badging, cert)

if __name__ == "__main__":
    unittest.main()
