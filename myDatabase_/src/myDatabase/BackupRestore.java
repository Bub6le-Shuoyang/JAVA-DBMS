package myDatabase;

import model.Database;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class BackupRestore {

    public static String backupDatabase(Database database) {
        try {
            // 获取当前数据库名
            String dbName = database.getCurrentDatabase();
            if (dbName == null || dbName.isEmpty()) {
                return "ERROR: 请先选择要备份的数据库";
            }

            // 弹出文件保存对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存数据库备份");
            fileChooser.setSelectedFile(new File(dbName + ".zip"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP备份文件", "zip"));

            if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                return "备份已取消";
            }

            File backupFile = fileChooser.getSelectedFile();

            String backupPath = backupFile.getAbsolutePath();
            if (!backupPath.toLowerCase().endsWith(".zip")) {
                backupPath += ".zip";
                backupFile = new File(backupPath);
            }

            // 获取数据库文件夹
            Path dbFolder = Paths.get(SQLConstant.getRootPath(), dbName);
            if (!Files.exists(dbFolder)) {
                return "ERROR: 数据库目录不存在: " + dbFolder;
            }

            // 执行压缩
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
                Files.walk(dbFolder)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                String entryName = dbFolder.relativize(path).toString()
                                        .replace(File.separator, "/");
                                zos.putNextEntry(new ZipEntry(entryName));
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                throw new RuntimeException("压缩文件失败: " + path, e);
                            }
                        });
            }

            return "Query OK: 备份成功 -> " + backupPath;
        } catch (Exception e) {
            return "ERROR: 备份失败 - " + e.getMessage();
        }
    }

    public static String restoreDatabase(Database database) {
        try {
            // 弹出文件选择对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("选择数据库备份文件");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP备份文件", "zip"));

            if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                return "恢复已取消";
            }

            File backupFile = fileChooser.getSelectedFile();
            String backupPath = backupFile.getAbsolutePath();

            // 从文件名获取数据库名
            String dbName = backupFile.getName().replace(".zip", "");

            // 准备解压路径
            Path dbPath = Paths.get(SQLConstant.getRootPath(), dbName);

            // 如果数据库已存在，先删除
            if (Files.exists(dbPath)) {
                deleteDirectory(dbPath.toFile());
            }

            // 创建目标目录
            Files.createDirectories(dbPath);

            // 执行解压
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    Path newPath = dbPath.resolve(entry.getName());
                    // 确保父目录存在
                    Files.createDirectories(newPath.getParent());
                    // 写入文件
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 刷新数据库列表
            database.refreshDatabaseList();

            return "Query OK: 数据库 '" + dbName + "' 还原成功";
        } catch (Exception e) {
            return "ERROR: 还原失败 - " + e.getMessage();
        }
    }

    private static boolean deleteDirectory(File directory) {
        if (!directory.exists()) return true;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
}