import importlib.util
import pathlib
import unittest
from unittest import mock


ROOT = pathlib.Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("adb_closed", ROOT / "tools" / "adb-closed.py")
adb_closed = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(adb_closed)


class ProbeTests(unittest.TestCase):
    def test_refused_port_is_confirmed_closed(self):
        sock = mock.Mock()
        sock.connect.side_effect = ConnectionRefusedError()
        with mock.patch.object(adb_closed.socket, "socket", return_value=sock):
            self.assertEqual(adb_closed.probe("127.0.0.1", 5555), "refused (verified closed)")
        sock.close.assert_called_once()

    def test_timeout_is_inconclusive(self):
        sock = mock.Mock()
        sock.connect.side_effect = TimeoutError("timed out")
        with mock.patch.object(adb_closed.socket, "socket", return_value=sock):
            self.assertIn("inconclusive", adb_closed.probe("127.0.0.1", 5555))

    def test_main_returns_failure_if_any_port_is_not_refused(self):
        with mock.patch.object(adb_closed, "probe", side_effect=["refused (verified closed)", "open"]):
            with mock.patch.object(adb_closed.sys, "argv", ["adb-closed.py", "127.0.0.1"]):
                with self.assertRaises(SystemExit) as raised:
                    adb_closed.main()
        self.assertEqual(raised.exception.code, 1)


if __name__ == "__main__":
    unittest.main()
