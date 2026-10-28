import importlib.util
import pathlib
import tempfile
import unittest
from types import SimpleNamespace
from unittest import mock


ROOT = pathlib.Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("tv", ROOT / "tools" / "tv.py")
tv = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(tv)


class TvToolTests(unittest.TestCase):
    def test_key_aliases_are_mapped(self):
        self.assertEqual(tv.keyname("left"), "KEYCODE_DPAD_LEFT")
        self.assertEqual(tv.keyname("custom"), "custom")

    def test_invalid_screenshot_does_not_overwrite_existing_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = pathlib.Path(temp_dir) / "capture.png"
            path.write_bytes(b"previous good image")
            result = SimpleNamespace(returncode=0, stdout=b"not a png", stderr=b"")
            with mock.patch.object(tv, "HOST", "127.0.0.1"), \
                    mock.patch.object(tv, "adb", return_value=result), \
                    mock.patch.object(tv.sys, "exit", side_effect=SystemExit):
                with self.assertRaises(SystemExit):
                    tv.screenshot(str(path), [])
            self.assertEqual(path.read_bytes(), b"previous good image")

    def test_valid_screenshot_is_written(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            path = pathlib.Path(temp_dir) / "nested" / "capture.png"
            data = tv.PNG_MAGIC + b"x" * 80
            result = SimpleNamespace(returncode=0, stdout=data, stderr=b"")
            with mock.patch.object(tv, "HOST", "127.0.0.1"), \
                    mock.patch.object(tv, "adb", return_value=result):
                tv.screenshot(str(path), [])
            self.assertEqual(path.read_bytes(), data)

    def test_connect_checks_exit_status(self):
        result = SimpleNamespace(returncode=1, stdout="connected to 127.0.0.1:5555", stderr="error")
        with mock.patch.object(tv.subprocess, "run", return_value=result), \
                mock.patch.object(tv.sys, "exit", side_effect=SystemExit):
            with self.assertRaises(SystemExit):
                tv.connect()


if __name__ == "__main__":
    unittest.main()
