import java.util.ArrayList;
import java.util.Scanner;

public class UniqueNumbers {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter your numbers: ");
        String input = scanner.nextLine();
        
        // تقسيم المدخلات بناءً على المسافات 
        String[] nums = input.split("\\s+");
        
        ArrayList<String> unique = new ArrayList<>();
        
        for (String item : nums) {
            if (!unique.contains(item)) {
                unique.add(item);
            }
        }
        System.out.println(unique);       
        scanner.close();
    }
}
