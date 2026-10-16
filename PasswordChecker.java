import java.util.Scanner;

public class PasswordChecker {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();
        
        // شروط أولية
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasNumber = false;
        boolean hasEnough = password.length() >= 8;
        
        // حلقة للتحقق من الرموز الحرفية
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            }
            if (Character.isLowerCase(ch)) {
                hasLower = true;
            }
            if (Character.isDigit(ch)) {
                hasNumber = true;
            }
        }
        
        // التحقق النهائي والطباعة
        if (hasEnough && hasNumber && hasLower && hasUpper) {
            System.out.println("Strong password");
        } else {
            System.out.println("Weak password");
            if (!hasEnough) {
                System.out.println("At least 8 char");
            }
            if (!hasUpper) {
                System.out.println("At least 1 upper");
            }
            if (!hasLower) {
                System.out.println("At least 1 lower");
            }
            if (!hasNumber) {
                System.out.println("At least 1 number");
            }
        }
        
        scanner.close();
    }
}