from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import server_metrics as metrics


class MetricsTest(unittest.TestCase):
    def test_proc_parsing_primary_interface_and_available_memory(self):
        with tempfile.TemporaryDirectory() as temp:
            proc=Path(temp);(proc/'net').mkdir()
            (proc/'stat').write_text('cpu  100 20 30 400 10 4 6 8 90 10\n')
            (proc/'meminfo').write_text('MemTotal: 10000 kB\nMemFree: 100 kB\nMemAvailable: 2500 kB\n')
            def row(name,rx,tx):return name+': '+str(rx)+' 0 0 0 0 0 0 0 '+str(tx)+' 0 0 0 0 0 0 0\n'
            (proc/'net/dev').write_text('header\nheader\n'+row('lo',999,999)+row('ens3',1000,2000)+row('bridge0',5000,6000))
            (proc/'net/route').write_text('Iface Destination Gateway Flags RefCnt Use Metric Mask\nens3 00000000 01000000 0003 0 0 100 00000000\n')
            sample=metrics.read_sample(proc)
            self.assertEqual(sample['cpuTotal'],578)  # Guest time is already included in user/nice.
            self.assertEqual(sample['cpuIdle'],410)
            self.assertEqual(sample['ramUsedBytes'],7500*1024)
            self.assertEqual(sample['network'],{'ens3':(1000,2000)})
            (proc/'meminfo').write_text('MemTotal: 10000 kB\nMemAvailable: 10001 kB\n')
            with self.assertRaises(ValueError):metrics.read_sample(proc)

    def test_interval_rates_and_public_projection(self):
        first=dict(clock=10,cpuTotal=1000,cpuIdle=700,ramUsedBytes=300,ramTotalBytes=1000,network={'ens3':(1000,2000)})
        second=dict(first,clock=15,cpuTotal=1200,cpuIdle=750,network={'ens3':(9000,4000)})
        with patch('server_metrics.time.time',return_value=123):
            value=metrics.summarize(second,first)
        self.assertEqual(value,dict(updated=123,cpuPercent=75.0,ramUsedBytes=300,ramTotalBytes=1000,rxBytesPerSecond=1600,txBytesPerSecond=400))
        self.assertIsNone(metrics.summarize(first)['cpuPercent'])
        self.assertIsNone(metrics.summarize(first)['rxBytesPerSecond'])

    def test_counter_resets_and_interface_changes_do_not_spike(self):
        first=dict(clock=10,cpuTotal=1000,cpuIdle=700,ramUsedBytes=300,ramTotalBytes=1000,network={'ens3':(1000,2000)})
        reset=dict(first,clock=15,cpuTotal=500,cpuIdle=300,network={'ens3':(10,20)})
        value=metrics.summarize(reset,first)
        self.assertIsNone(value['cpuPercent']);self.assertIsNone(value['rxBytesPerSecond'])
        renamed=dict(first,clock=15,network={'new':(100000000,100000000)})
        self.assertIsNone(metrics.summarize(renamed,first)['txBytesPerSecond'])


if __name__=='__main__':unittest.main()
