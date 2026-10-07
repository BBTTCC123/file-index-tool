package chen;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Dirscanner dirscanner = new Dirscanner();
        Renamer renamer = new Renamer();
        try(Scanner sc = new Scanner(System.in);){

            while(true){
                System.out.println("============文件管理系统============");
                System.out.println("||1.录入文件                      ||");
                System.out.println("||2.查询文件                      ||");
                System.out.println("||3.查看所有文件                  ||");
                System.out.println("||4.文件类型统计                  ||");
                System.out.println("||5.删除文件                      ||");
                System.out.println("||6.递归录入文件                  ||");
                System.out.println("||7.重命名计划(dry-run)           ||");
                System.out.println("||8.执行重命名                    ||");
                System.out.println("||0.退出                          ||");
                System.out.println("||ps:路径长度不应超过100字符     ||");
                System.out.println("====================================");

                int n = sc.nextInt();
                sc.nextLine();
                if(n==0){break;}
                else if(n==1){
                    System.out.println("请输入要录入的文件(夹)的绝对路径: ");
                    String path = sc.nextLine();
                    dirscanner.scan(path);
                }
                else if(n==2){
                    System.out.println("请输入要查询的文件(夹)的绝对路径: ");
                    String name = sc.nextLine();
                    dirscanner.jdbcm.query(name);
                }
                else if(n==3){
                    dirscanner.traverseQuery();
                }
                else if(n==4){
                    dirscanner.typeQuery();
                }
                else if(n==5){
                    System.out.println("请输入要删除的文件(夹)的绝对路径: ");
                    String p = sc.nextLine();
                    try {
                        dirscanner.delete(p);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                else if(n==6){
                    System.out.println("请输入要录入的文件(夹)的绝对路径: ");
                    String path = sc.nextLine();
                    dirscanner.reScan(path);
                }
                else if(n==7){
                    System.out.println("请输入要整理的文件(夹)的绝对路径: ");
                    String path = sc.nextLine();
                    List<Renamer.Plan> plan = renamer.buildPlan(path);
                    if (plan.isEmpty()) {
                        System.out.println("文件或文件夹不存在，或者里面一个文件都没有");
                    } else {
                        System.out.println("========== 重命名计划 (dry-run) ==========");
                        renamer.printPlan(plan);
                        System.out.println("计划已完成，文件尚未修改");
                    }
                }
                else if(n==8){
                    System.out.println("请输入要重命名的文件(夹)的绝对路径: ");
                    String path = sc.nextLine();
                    List<Renamer.Plan> plan = renamer.buildPlan(path);
                    if (plan.isEmpty()) {
                        System.out.println("文件或文件夹不存在，或者里面一个文件都没有");
                    } else {
                        renamer.printPlan(plan);
                        System.out.println("请问是否确认重命名该列表（y/n）?:");
                        String s = sc.nextLine();
                        if (s.equals("y")||s.equals("Y")) {
                            renamer.renameFile(plan);
                        }else if (s.equals("n")||s.equals("N")) {
                            System.out.println("文件重命名已取消");
                        }
                    }
                }
            }
            dirscanner.jdbcm.close();
        }


    }
}
