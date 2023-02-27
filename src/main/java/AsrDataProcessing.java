import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AsrDataProcessing {
    private Map<String, Map<String, String>> speakerMap = new HashMap<>();
    private Map<String, String> sessionMap = new HashMap<>();
    private Map<String, String> contentMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        AsrDataProcessing asrdataProcessing = new AsrDataProcessing();
        List<AsrdataInfo> asrdataInfoList = new ArrayList<>();

        // 判断 voice 目录是否存在
        File voice = new File("/Users/liuyang/Desktop/AsrDataProcessing/voice");
        if (!voice.exists()) {
            voice.mkdir();
        }

        // 1.读取 SPEAKER.xlsx ，初始化 speakerMap
        asrdataProcessing.initialSPEAKER();
//        System.out.println(audioProcessing.speakerMap);

        // 2.读取 SESSION.xlsx ，初始化 sessionMap
        asrdataProcessing.initialSESSION();
//        System.out.println(audioProcessing.sessionMap);

        // 3.读取 data\CHANNEL1\SCRIPT 下所有的 TXT 文件， 初始化 contentMap
        asrdataProcessing.initialCONTENT();
//        System.out.println(audioProcessing.contentMap);

        // 4.查找 data\CHANNEL1\WAVE 下所有的 SPEAKER***
        //String wavePath = "data" + File.separator + "CHANNEL1" + File.separator + "WAVE";
        String wavePath = "/Users/liuyang/Desktop/AsrDataProcessing/CHANNEL1/WAVE";
        File parentDirectory = new File(wavePath);
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("SPEAKER");
            }
        };
        File[] speakerFiles = parentDirectory.listFiles(filenameFilter);

        // 5.针对每一个 SPEAKER*** ，截取字符串，得到 speaker id （***）
        for (int i = 0; i < speakerFiles.length; i++) {
            String speakerId = speakerFiles[i].getName().substring(7);
//            System.out.println(speakerId);
            // 6.查找 data\CHANNEL1\WAVE\SPEAKER*** 下的所有 SESSION***
            String speakerPath = wavePath + File.separator + speakerFiles[i].getName();
            File sessionParentDirectory = new File(speakerPath);
            FilenameFilter filenameFilter1 = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("SESSION");
                }
            };
            File[] sessionFiles = sessionParentDirectory.listFiles(filenameFilter1);
            for (int j = 0; j < sessionFiles.length; j++) {
//                System.out.println(sessionFiles[j]);
                // 7.查找 data\CHANNEL1\WAVE\SPEAKER***\SESSION*** 下所有的 wav文件
                String sessionPath = speakerPath + File.separator + sessionFiles[j].getName();
                File wavParentDirectory = new File(sessionPath);
                FilenameFilter filenameFilter2 = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith("wav");
                    }
                };
                File[] wavFiles = wavParentDirectory.listFiles(filenameFilter2);
                for (int k = 0; k < wavFiles.length; k++) {
                    //                   System.out.println(wavFiles[k]);
                    // 8.构造 AudioInfo 对象
                    AsrdataInfo asrdataInfo = new AsrdataInfo();
                    // id 只是编号，不要后缀名
                    String id = wavFiles[k].getName().substring(0, wavFiles[k].getName().lastIndexOf("."));
                    asrdataInfo.setId(id);
                    asrdataInfo.setSpeakerId(speakerId);
                    asrdataInfo.setSessionId(sessionFiles[j].getName());
                    asrdataInfo.setName(wavFiles[k].getName());
                    Map<String, String> tags = asrdataProcessing.speakerMap.get(speakerId);
                    tags.put("environ", asrdataProcessing.sessionMap.get(sessionFiles[j].getName()));
                    tags.put("scene", "");  // TODO
                    asrdataInfo.setTags(tags);
                    asrdataInfo.setContent(asrdataProcessing.contentMap.get(id));
                    asrdataInfo.setPath("/Users/liuyang/Desktop/AsrDataProcessing/voice/" + wavFiles[k].getName());
                    System.out.println("/Users/liuyang/Desktop/AsrDataProcessing/voice/" + wavFiles[k].getName());
                    // 9.保存到 List 中
                    asrdataInfoList.add(asrdataInfo);

                    // 10.复制音频文件到 voice
                    FileUtils.copyFile(wavFiles[k], new File(asrdataInfo.getPath()));
                }
            }
        }
        // voice打包为zip
        System.out.println("----开始打包----");
        String packagePath = "/Users/liuyang/Desktop/AsrDataProcessing/voice";  //选中的文件夹
        asrdataProcessing.packageZip(packagePath);
        System.out.println("----完成打包----");
        // 11.写入到excel中
        asrdataProcessing.writeExcel(asrdataInfoList);
        System.out.println("----完成写入----");
    }

    private void initialSPEAKER() throws IOException {
        String filepath = "SPEAKER.xlsx";
        Workbook wb = null;
        try {
            //String encoding = "GBK";
            File excel = new File(filepath);
            if (excel.isFile() && excel.exists()) {   //判断文件是否存在
                wb = new XSSFWorkbook(excel);
                Sheet sheet = wb.getSheetAt(0);     //读取第一个sheet

                int firstRowIndex = sheet.getFirstRowNum() + 1;   // 从第2行开始处理
                int lastRowIndex = sheet.getLastRowNum();

                for(int rIndex = firstRowIndex; rIndex <= lastRowIndex; rIndex++) {
                    Row row = sheet.getRow(rIndex);
                    if (row != null) {
                        String scd = row.getCell(0).getStringCellValue();
                        String sex = row.getCell(1).getStringCellValue();
                        String age = String.valueOf((int)row.getCell(2).getNumericCellValue());
                        String acc = row.getCell(3).getStringCellValue();
                        Map<String, String> temp = new HashMap<>();
                        temp.put("sex", sex);
                        temp.put("age", age);
                        temp.put("language", getLanguage(acc));
                        speakerMap.put(scd, temp);
                        //                       System.out.println(speakerMap);
                    }
                }
            } else {
                System.out.println("找不到SPEAKER.xlsx文件");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wb != null) {
                wb.close();
            }
        }
    }

    private void initialSESSION() throws IOException {
        String filepath = "SESSION.xlsx";
        Workbook wb = null;
        try {
            //String encoding = "GBK";
            File excel = new File(filepath);
            if (excel.isFile() && excel.exists()) {   //判断文件是否存在
                wb = new XSSFWorkbook(excel);
                Sheet sheet = wb.getSheetAt(0);     //读取第一个sheet

                int firstRowIndex = sheet.getFirstRowNum() + 1;   // 从第2行开始处理
                int lastRowIndex = sheet.getLastRowNum();

                for(int rIndex = firstRowIndex; rIndex <= lastRowIndex; rIndex++) {
                    Row row = sheet.getRow(rIndex);
                    if (row != null) {
                        String ses = row.getCell(0).getStringCellValue();
                        String environment = row.getCell(1).getStringCellValue();
                        sessionMap.put(ses, environment);
//                        System.out.println(speakerMap);
                    }
                }
            } else {
                System.out.println("SESSION.xlsx文件");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (wb != null) {
                wb.close();
            }
        }
    }

    private void initialCONTENT() throws IOException {
        //String path = "data" + File.separator + "CHANNEL1" + File.separator + "SCRIPT";
        String path = "/Users/liuyang/Desktop/AsrDataProcessing/CHANNEL1/SCRIPT";
        File parentDirectory = new File(path);
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("txt");
            }
        };
        File[] files = parentDirectory.listFiles(filenameFilter);
        // 迭代 data\CHANNEL1\SCRIPT 下所有的 TXT 文件
        for (int i = 0; i < files.length; i++) {
            FileInputStream fileInputStream = new FileInputStream(files[i]);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line = null;
            int j = 0;
            while ((line = bufferedReader.readLine()) != null) {
                ++j;
                if (j % 2 == 0) {  // 忽略偶数行
                    continue;
                }

                String[] datas = line.split("\\t");
                contentMap.put(datas[0], datas[1]);
            }
            bufferedReader.close();
        }
    }

    private String getLanguage(String acc) {
        // TODO 暂时没明确如何操作 acc
        if (acc.startsWith("China")) {
            return "普通话";
        }

        return "普通话";
    }

    private void packageZip(String filesPath) throws Exception {
        // 要被压缩的文件夹
        File file = new File(filesPath);   //需要压缩的文件夹
        File zipFile = new File(filesPath+".zip");  //放于和需要压缩的文件夹同级目录
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
        isDirectory(file,zipOut,"",true);   //判断是否为文件夹
        zipOut.close();
    }

    private void isDirectory(File file, ZipOutputStream zipOutputStream, String filePath, boolean flag) throws IOException {
        //判断是否为问加减
        if(file.isDirectory()){
            File[] files = file.listFiles();  //获取该文件夹下所有文件(包含文件夹)
            filePath = flag==true?file.getName():filePath + File.separator + file.getName();   //首次为选中的文件夹，即根目录，之后递归实现拼接目录
            for(int i = 0; i < files.length; ++i){
                //判断子文件是否为文件夹
                if(files[i].isDirectory()){
                    //进入递归,flag置false 即当前文件夹下仍包含文件夹
                    isDirectory(files[i],zipOutputStream,filePath,false);
                }else{
                    //不为文件夹则进行压缩
                    InputStream input = new FileInputStream(files[i]);
                    zipOutputStream.putNextEntry(new ZipEntry(filePath+File.separator+files[i].getName()));
                    int temp = 0;
                    while((temp = input.read()) != -1){
                        zipOutputStream.write(temp);
                    }
                    input.close();
                }
            }
        }else{
            //将子文件夹下的文件进行压缩
            InputStream input = new FileInputStream(file);
            zipOutputStream.putNextEntry(new ZipEntry(file.getPath()));
            int temp = 0;
            while((temp = input.read()) != -1){
                zipOutputStream.write(temp);
            }
            input.close();
        }
    }



    private void writeExcel(List<AsrdataInfo> asrdataInfoList) {
        Workbook workbook = null;
        FileOutputStream fileOut = null;
        try {
            workbook = new SXSSFWorkbook();
            Sheet sheet = workbook.createSheet();
            sheet.setColumnWidth(0, 8157);
            sheet.setColumnWidth(1, 22116);
            sheet.setColumnWidth(2, 5400);
            sheet.setColumnWidth(3, 12544);

            String tianxiexuzhi = "【填写须知】：\n" +
                    "1、请勿修改当前模板结构。\n" +
                    "2、红色字段必填，黑色字段按照实际情况选填。\n" +
                    "3、所有的音频文件都必须放在voice目录下打包。\n" +
                    "4、file_name字段值为带路径的文件名。";
            // 创建一个合并单元格
            CellRangeAddress region = CellRangeAddress.valueOf("A1:D1");
            sheet.addMergedRegion(region);
            Cell cell = sheet.createRow(0).createCell(0);
            cell.setCellValue(tianxiexuzhi);
            // 设置单元格内容自动换行
            CellStyle huanhangStyle = workbook.createCellStyle();
            huanhangStyle.setWrapText(true);
            cell.setCellStyle(huanhangStyle);
            // 设置单元格内容垂直居中
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);

            // 设置第一行的行高，89磅
            sheet.getRow(0).setHeightInPoints(89);

            // 设置字体颜色
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setColor(Font.COLOR_RED);
            style.setFont(font);

            Row head = sheet.createRow(1);
            cell = head.createCell(0);
            cell.setCellValue("content");
            cell.setCellStyle(style);

            cell = head.createCell(1);
            cell.setCellValue("tags");
            cell.setCellStyle(style);

            cell = head.createCell(2);
            cell.setCellValue("extend_info");

            cell = head.createCell(3);
            cell.setCellValue("file_name");
            cell.setCellStyle(style);

            for (int i = 0; i < asrdataInfoList.size(); i++) {
                head = sheet.createRow(2 + i);
                cell = head.createCell(0);
                cell.setCellValue(asrdataInfoList.get(i).getContent());

                cell = head.createCell(1);
                cell.setCellValue(JSONObject.toJSONString(asrdataInfoList.get(i).getTags(), JSONWriter.Feature.WriteNulls));

                cell = head.createCell(2);
                cell.setCellValue(asrdataInfoList.get(i).getExtand_info());

                cell = head.createCell(3);
                cell.setCellValue(asrdataInfoList.get(i).getPath());
            }

            // 导出的结果文件
            fileOut = new FileOutputStream("/Users/liuyang/Desktop/AsrDataProcessing/voice_data.xlsx");
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOut != null) {
                    fileOut.close();
                }

                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
