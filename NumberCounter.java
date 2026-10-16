import java.util.ArrayList;
import java.util.Scanner;

public class NumberCounter {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("enter number separated by space: ");
        String input = scanner.nextLine();
        
        // تقسيم المدخلات بناءً على المسافات
        String[] numbers = input.split("\\s+");
        ArrayList<Integer> newList = new ArrayList<>();
        
        // تحويل النصوص إلى أقام صحيحة وإضافتها للقائمة
        for (String numStr : numbers) {
            if (!numStr.isEmpty()) {
                newList.add(Integer.parseInt(numStr));
            }
        }
        
        System.out.println("NewList " + newList);
        
        int positive = 0;
        int negative = 0; // تم تصحيح الإملاء من nigative إلى negative
        int zero = 0;
        
        // حلقة للتحقق من نوع الأرقام
        for (int num : newList) {
            if (num > 0) {
                positive++;
            } else if (num < 0) {
                negative++;
            } else {
                zero++;
            }
        }
        
        // طباعة النتائج
        System.out.println("+ " + positive);
        System.out.println("- " + negative);
        System.out.println("zero " + zero);
        
        scanner.close();
    }
}