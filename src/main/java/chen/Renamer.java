package chen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Renamer {
    // 计划表里的一行
    static class Plan {
        String from;    // 原路径
        String to;      // 算出来的新路径（还没做）
        String status;  // "改" / "大小写" / "冲突" / "跳过"
        String reason;  // 跳过和冲突时写明原因，其他为空
    }

    // 规范名的前缀恒定大写
    private static final String PREFIX = "Lab";
    // 受保护目录的根。末尾这个 '\' 不能省，否则 reports_backup 也会被当成 reports
    private static final String ROOT = "E:\\Java_Labs_study\\reports\\";
    // labNN / LabNN（大小写随意），或者 实验X（中文数字）
    private static final Pattern EXP_PATTERN = Pattern.compile("(?i)lab\\d{2}|实验([一二三四五六七八九十]+)");
    private static final String CN_DIGITS = "一二三四五六七八九";
    // 改名前的备份、改名后的记录都放这里。写死绝对路径，放在 reports 旁边
    // 注意不能放进 reports 里面，否则下次扫描会把备份和记录当成待整理文件
    private static final String LOG_DIR = "E:\\Java_Labs_study\\rename_logs";

    // 第一步：算出整张表。只读文件系统，不打印，不改文件
    public List<Plan> buildPlan(String path) {
        List<Plan> planList = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            return planList;   // 只读不打印，路径不对就把空表交回去，由调用方去说
        }
        collect(file, planList);
        markConflicts(planList);
        planList.sort((a, b) -> {
            int byStatus = Integer.compare(statusOrder(a.status), statusOrder(b.status));
            return (byStatus != 0) ? byStatus : keyOf(a).compareTo(keyOf(b));
        });
        return planList;
    }

    // 排序用：先按状态分组，组内按算出来的新名字排；跳过的没有新名字，就用原路径
    private String keyOf(Plan p) {
        return p.to.isEmpty() ? p.from : p.to;
    }

    private int statusOrder(String status) {
        if ("改".equals(status)) {
            return 0;
        }
        if ("大小写".equals(status)) {
            return 1;
        }
        if ("冲突".equals(status)) {
            return 2;
        }
        return 3;
    }

    // 计划内撞名：两个源文件算出同一个目标。逐条判不出来，得整张表攒完一起看
    private void markConflicts(List<Plan> planList) {
        Map<String, List<Plan>> byTo = new HashMap<>();
        for (Plan p : planList) {
            if (!p.to.isEmpty()) {
                byTo.computeIfAbsent(p.to, k -> new ArrayList<>()).add(p);
            }
        }
        for (List<Plan> group : byTo.values()) {
            if (group.size() > 1) {
                for (Plan p : group) {
                    p.status = "冲突";
                    p.reason = "计划内撞名，有 " + group.size() + " 个文件都想叫 " + new File(p.to).getName();
                }
            }
        }
    }

    // 递归只负责往下走，表只有一张，一路往里加
    private void collect(File file, List<Plan> planList) {
        if (!file.isDirectory()) {
            planList.add(makePlan(file, namesIn(file.getParentFile())));
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return;   // 没权限，当空目录
        }
        // 同一个目录里的名字先收齐，"冲突"才判得出来
        Set<String> names = new HashSet<>();
        for (File f : files) {
            names.add(f.getName());
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, planList);
            } else {
                planList.add(makePlan(f, names));
            }
        }
    }

    private Set<String> namesIn(File dir) {
        Set<String> names = new HashSet<>();
        if (dir == null) {
            return names;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return names;
        }
        for (File f : files) {
            names.add(f.getName());
        }
        return names;
    }

    private Plan makePlan(File f, Set<String> siblingNames) {
        Plan p = new Plan();
        p.from = f.getAbsolutePath();
        p.to = "";
        p.reason = "";

        // 受保护目录：名字被 build.py / labs_data_b.py 按名引用，改了会断链
        if (p.from.startsWith(ROOT + "docx\\") || p.from.startsWith(ROOT + "flowcharts\\")) {
            p.status = "跳过";
            p.reason = "受保护目录";
            return p;
        }

        // 切名字和扩展名。扩展名带着点，最后原样拼回去
        String wholeName = f.getName();
        int dot = wholeName.lastIndexOf(".");
        String fileName = (dot <= 0) ? wholeName : wholeName.substring(0, dot);
        String extName = (dot <= 0) ? "" : wholeName.substring(dot);

        // 认实验号
        Matcher m = EXP_PATTERN.matcher(fileName);
        if (!m.find()) {
            p.status = "跳过";
            p.reason = "认不出实验号";
            return p;
        }
        int num;
        if (m.group(1) == null) {
            num = Integer.parseInt(m.group().substring(3));   // lab01 去掉前三个字符 -> 01 -> 1
        } else {
            num = cnToInt(m.group(1));                        // 七 -> 7，十六 -> 16
        }

        // 把认出实验号的那一段换成 Lab + 两位编号，前后照抄
        String newName = fileName.substring(0, m.start())
                + PREFIX + String.format("%02d", num)
                + fileName.substring(m.end())
                + extName;

        // 新名字先存好，判成冲突时也要能看见它想叫什么叫
        File parent = f.getParentFile();
        p.to = (parent == null) ? newName : new File(parent, newName).getAbsolutePath();

        if (newName.equals(wholeName)) {
            p.status = "跳过";
            p.reason = "已经是规范名";
            p.to = "";
            return p;
        }
        if (siblingNames != null && siblingNames.contains(newName)) {
            p.status = "冲突";
            p.reason = "目标名已存在";
            return p;
        }

        p.status = newName.equalsIgnoreCase(wholeName) ? "大小写" : "改";
        return p;
    }

    // 中文数字转阿拉伯数字，够用到九十九
    private int cnToInt(String cn) {
        int ten = cn.indexOf('十');
        if (ten < 0) {
            return CN_DIGITS.indexOf(cn.charAt(0)) + 1;
        }
        int tens = (ten == 0) ? 1 : CN_DIGITS.indexOf(cn.charAt(0)) + 1;
        if (ten == cn.length() - 1) {
            return tens * 10;
        }
        return tens * 10 + CN_DIGITS.indexOf(cn.charAt(ten + 1)) + 1;
    }

    // 第二步：把表打印出来 + 打汇总
    public void printPlan(List<Plan> planList) {
        int change = 0;
        int caseOnly = 0;
        int conflict = 0;
        int skip = 0;
        Map<String, Integer> skipReasons = new LinkedHashMap<>();

        for (Plan p : planList) {
            String from = p.from.startsWith(ROOT) ? p.from.substring(ROOT.length()) : p.from;
            String line = "[" + p.status + "] " + from;
            if (!p.to.isEmpty()) {
                line = line + " -> " + new File(p.to).getName();
            }
            if (!p.reason.isEmpty()) {
                line = line + "   (" + p.reason + ")";
            }
            System.out.println(line);
            if ("改".equals(p.status)) {
                change++;
            } else if ("大小写".equals(p.status)) {
                caseOnly++;
            } else if ("冲突".equals(p.status)) {
                conflict++;
            } else {
                skip++;
                skipReasons.merge(p.reason, 1, Integer::sum);
            }
        }
        StringBuilder detail = new StringBuilder();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(skipReasons.entrySet());
        entries.sort((a, b) -> (a.getValue().equals(b.getValue()))
                ? a.getKey().compareTo(b.getKey())
                : Integer.compare(b.getValue(), a.getValue()));   // 条数多的先说
        for (Map.Entry<String, Integer> e : entries) {
            if (detail.length() > 0) {
                detail.append(" / ");
            }
            detail.append(e.getKey()).append(" ").append(e.getValue());
        }
        String skipPart = (skip == 0) ? String.valueOf(skip) : skip + "（" + detail + "）";
        System.out.println("共 " + planList.size() + " 个文件：改 " + change + "，大小写 " + caseOnly
                + "，冲突 " + conflict + "，跳过 " + skipPart);
    }


    // 第三步：真的改名 —— 今天不写，方法先别加
    public void renameFile(List<Plan> planList) {
        List<Plan> renameList = new ArrayList<>();
        String timeStampName;
        int change = 0;
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // 备份必须排在第一个 move 之前；会不会被覆盖，全靠这一份
        String backupFile = writeBackup(planList, stamp);
        if (backupFile.isEmpty()) {
            System.out.println("备份没写成，这次一个文件都不动");
            return;
        }
        System.out.println("改名前的备份：" + backupFile);
        for (Plan p : planList) {
            if(!("改".equals(p.status))&&!("大小写".equals(p.status))){
                System.out.println("由于 "+p.reason+",所以文件 "+p.from+" 未改名且已跳过");
                continue;
            }
            try {
                if("大小写".equals(p.status)){
                    timeStampName = p.to+"."+System.currentTimeMillis();
                    Files.move(Paths.get(p.from), Paths.get(timeStampName));
                    Plan rename = new Plan();
                    rename.to=timeStampName;
                    rename.from=p.from;
                    rename.reason=p.reason;
                    rename.status=p.status;
                    renameList.add(rename);
                    Files.move(Paths.get(timeStampName), Paths.get(p.to));
                    renameList.getLast().to=p.to;
                }else{
                    Files.move(Paths.get(p.from), Paths.get(p.to));
                    renameList.add(p);
                }
                change++;
            } catch (IOException e) {
                System.out.println("重命名失败，文件列表将回滚");
                if (!renameList.isEmpty()){
                    for(Plan r:renameList){
                        try {
                            if("大小写".equals(r.status)){
                                timeStampName = r.to+"."+System.currentTimeMillis();
                                Files.move(Paths.get(r.to), Paths.get(timeStampName));
                                Files.move(Paths.get(timeStampName), Paths.get(r.from));
                            }else{
                                Files.move(Paths.get(r.to), Paths.get(r.from));
                            }
                        } catch (IOException ex) {
                            System.out.println("发生致命错误，文件列表回滚失败");
                            throw new RuntimeException(ex);
                        }
                    }
                    System.out.println("回滚列表：");
                    printPlan(renameList);
                    System.out.println("以上改动已撤销");
                }else{
                    System.out.println("文件列表未发生改动");
                }
                writeResult(stamp, "失败，改动已回滚", renameList, e.getMessage());
                throw new RuntimeException(e);
            }
        }
        writeResult(stamp, "成功", renameList, "");
        System.out.println("重命名成功,改动 "+change+" 个文件");
    }

    // 把整张计划表落成文件。返回绝对路径；写不出来就返回空串，调用方据此决定动不动手
    private String writeBackup(List<Plan> planList, String stamp) {
        String name = LOG_DIR + File.separator + "backup_" + stamp + ".txt";
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(name), StandardCharsets.UTF_8)) {
                writer.write("改名备份  时间 " + stamp);
                writer.newLine();
                writer.write("共 " + planList.size() + " 条");
                writer.newLine();
                writer.write("状态\t原路径\t目标路径\t原因");
                writer.newLine();
                for (Plan p : planList) {
                    writer.write(p.status + "\t" + p.from + "\t" + p.to + "\t" + p.reason);
                    writer.newLine();
                }
            }
            return new File(name).getAbsolutePath();
        } catch (IOException e) {
            System.out.println("备份写入失败：" + e.getMessage());
            return "";
        }
    }

    // 记下这一趟实际改了什么。时间戳和备份用同一个，两个文件能配对
    private void writeResult(String stamp, String result, List<Plan> doneList, String note) {
        String name = LOG_DIR + File.separator + "result_" + stamp + ".txt";
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(name), StandardCharsets.UTF_8)) {
                writer.write("改名结果 " + result + "  时间 " + stamp);
                writer.newLine();
                if (note != null && !note.isEmpty()) {
                    writer.write("失败原因 " + note);
                    writer.newLine();
                }
                writer.write("实际改动 " + doneList.size() + " 条");
                writer.newLine();
                writer.write("原路径\t新路径");
                writer.newLine();
                for (Plan p : doneList) {
                    writer.write(p.from + "\t" + p.to);
                    writer.newLine();
                }
            }
            System.out.println("改动记录：" + new File(name).getAbsolutePath());
        } catch (IOException e) {
            System.out.println("改动记录写入失败：" + e.getMessage());
        }
    }
}
