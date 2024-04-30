package com.alphatica.alis.tools.java;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Zipper {

	private Zipper() {
	}

	public static void unzip(String file, String dstFolder) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			ZipEntry entry = zis.getNextEntry();
			while (entry != null) {
				String filePath = dstFolder + File.separator + entry.getName();
				if (entry.isDirectory()) {
					extractDirectory(filePath);
				} else {
					File dstFile = new File(filePath);
					verifyDstPath(dstFile, dstFolder);
					extractFile(zis, dstFile);
				}
				zis.closeEntry();
				entry = zis.getNextEntry();
			}
		}
	}

	private static void verifyDstPath(File dstFile, String dstFolder) throws IOException {
		if (!dstFile.getCanonicalPath().contains(dstFolder)) {
			throw new IllegalArgumentException(dstFile.getCanonicalPath() + " does not contain " + dstFolder);
		}
	}

	private static void extractDirectory(String filePath) throws IOException {
		File dir = new File(filePath);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Could not create directory " + dir.getAbsolutePath());
		}
	}

	private static void extractFile(ZipInputStream zipIn, File dstFile) throws IOException {
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dstFile))) {
			byte[] bytesIn = new byte[1024 * 1024];
			int read;
			while ((read = zipIn.read(bytesIn)) != -1) {
				bos.write(bytesIn, 0, read);
			}
		}
	}
}
