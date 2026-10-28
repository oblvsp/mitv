import pathlib
import unittest
import xml.etree.ElementTree as ET


ROOT = pathlib.Path(__file__).resolve().parents[1]


class AndroidResourceXmlTests(unittest.TestCase):
    def test_manifests_and_resource_xml_are_well_formed(self):
        files = list(ROOT.glob("*/AndroidManifest.xml"))
        files.extend(ROOT.glob("*/res/**/*.xml"))
        self.assertTrue(files)
        for path in files:
            with self.subTest(path=path.relative_to(ROOT)):
                ET.parse(path)


if __name__ == "__main__":
    unittest.main()
