"""Native export transport: truncated projects must never become saved sources."""
import json,unittest
from unittest.mock import patch
from proof import export
class ExportTests(unittest.TestCase):
    def client(self,result):
        class Client:
            def call(self,name,args):return {'content':[{'type':'text','text':json.dumps(result)}]}
        return Client()
    def test_complete_export_avoids_recompile(self):
        with patch('proof.evaluate') as evaluate:
            self.assertEqual(export(self.client({'content':'complete'}),'project'),'complete');evaluate.assert_not_called()
    def test_truncation_transfers_every_chunk_and_cleans_up(self):
        value='x'*120007
        with patch('proof.evaluate',side_effect=[len(value),value[:60000],value[60000:120000],value[120000:],True]) as evaluate:
            self.assertEqual(export(self.client({'truncated':True,'content':'partial'}),'project'),value)
            self.assertIn('delete globalThis.civilizationNativeExport',evaluate.call_args.args[1])
    def test_short_chunk_rejects_export_and_cleans_up(self):
        with patch('proof.evaluate',side_effect=[60001,'short',True]) as evaluate:
            with self.assertRaisesRegex(RuntimeError,'Incomplete native project export chunk'):export(self.client({'truncated':True}),'project')
            self.assertIn('delete globalThis.civilizationNativeExport',evaluate.call_args.args[1])
if __name__=='__main__':unittest.main()
