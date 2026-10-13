import java.util.Scanner;

public class SumCalculator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("enter your number: ");
        int num = scanner.nextInt();
        
        int total = 0;
        for (int i = 1; i <= num; i++) {
            total += i;
            System.out.println("the equal about sum " + total);
        }
        
        scanner.close();
    }
}