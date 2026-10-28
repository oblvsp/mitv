import os
import pathlib
import shutil
import subprocess
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
FAKE_ADB = r'''#!/usr/bin/env python3
import json, os, sys
args = sys.argv[1:]
with open(os.environ["ADB_LOG"], "a", encoding="utf-8") as log:
    log.write(json.dumps(args) + "\n")
if os.environ.get("FAIL_DISABLE") and "disable-user" in args:
    sys.exit(1)
if os.environ.get("FAIL_ENABLE") and "enable" in args:
    sys.exit(1)
if "list" in args and "packages" in args:
    print(os.environ.get("DISABLED_PACKAGES", ""))
'''


class PackageScriptTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temp.name)
        self.bin_dir = self.root / "bin"
        self.bin_dir.mkdir()
        self.adb = self.bin_dir / "adb"
        self.adb.write_text(FAKE_ADB, encoding="utf-8")
        self.adb.chmod(0o755)
        self.log = self.root / "adb.log"
        self.env = os.environ.copy()
        self.env.update({
            "PATH": str(self.bin_dir) + os.pathsep + os.environ.get("PATH", ""),
            "TV_HOST": "127.0.0.1",
            "ADB_LOG": str(self.log),
        })

    def tearDown(self):
        self.temp.cleanup()

    def run_script(self, script, *args, env=None):
        return subprocess.run(
            ["bash", str(script), *args],
            env=env or self.env,
            text=True,
            capture_output=True,
            check=False,
        )

    def test_disable_refuses_protected_and_invalid_names_without_adb(self):
        result = self.run_script(ROOT / "tools" / "disable.sh", "com.android.settings", "bad;name")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("REFUSED", result.stdout + result.stderr)
        self.assertFalse(self.log.exists())

    def test_disable_reports_verified_success(self):
        env = self.env.copy()
        env["DISABLED_PACKAGES"] = "package:com.example.app"
        result = self.run_script(ROOT / "tools" / "disable.sh", "com.example.app", env=env)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("OK disabled: com.example.app", result.stdout)

    def test_disable_propagates_adb_failure(self):
        env = self.env.copy()
        env["FAIL_DISABLE"] = "1"
        result = self.run_script(ROOT / "tools" / "disable.sh", "com.example.app", env=env)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("FAILED to disable", result.stderr)

    def test_restore_propagates_package_failure(self):
        script_dir = self.root / "restore"
        script_dir.mkdir()
        script = script_dir / "restore.sh"
        shutil.copy(ROOT / "backup" / "restore.sh", script)
        (script_dir / "packages-disabled-final.txt").write_text(
            "package:com.example.good\npackage:com.example.fail\n", encoding="utf-8"
        )
        env = self.env.copy()
        env["FAIL_ENABLE"] = "1"
        result = self.run_script(script, env=env)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("FAILED to enable: com.example.good", result.stderr)
        self.assertIn("completed with errors", result.stderr)


if __name__ == "__main__":
    unittest.main()
