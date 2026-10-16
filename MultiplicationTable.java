import java.util.Scanner;

public class MultiplicationTable {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("enter your num: ");
        double num = scanner.nextDouble();
        
        for (int loop = 1; loop < 13; loop++) {
            System.out.println(num + " x " + loop + " = " + (num * loop));
        }
        
        scanner.close();
    }
}