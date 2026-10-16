import java.util.Scanner;

public class LargestNumber {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("enter your first number: ");
        double num1 = scanner.nextDouble();
        
        System.out.print("enter your second number: ");
        double num2 = scanner.nextDouble();
        
        System.out.print("enter your third number: ");
        double num3 = scanner.nextDouble();
        
        if (num1 > num2 && num1 > num3) {
            System.out.println(num1 + " is the largest number");
        } else if (num2 > num1 && num2 > num3) {
            System.out.println(num2 + " is the largest number");
        } else {
            System.out.println(num3 + " is the largest number");
        }
        
        scanner.close();
    }
}