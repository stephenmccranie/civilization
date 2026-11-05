"""Follow Minecraft's active log through rename rotation and copy truncation."""
import os
from pathlib import Path


class LogTail:
    def __init__(self,path):
        self.path=Path(path)
        self.file=None

    def close(self):
        if self.file:
            self.file.close()
            self.file=None

    def read(self):
        lines=[]
        if self.file:
            lines.extend(self.file.readlines())
        try:
            stat=self.path.stat()
        except FileNotFoundError:
            return lines
        if self.file is None or (os.fstat(self.file.fileno()).st_dev,os.fstat(self.file.fileno()).st_ino)!=(stat.st_dev,stat.st_ino):
            self.close()
            self.file=self.path.open(errors='replace')
            lines.extend(self.file.readlines())
        elif self.file.tell()>stat.st_size:
            self.file.seek(0)
            lines.extend(self.file.readlines())
        return lines

    def discard(self):
        """Skips the lines written so far, returning them for anything that must still see them."""
        return self.read()
