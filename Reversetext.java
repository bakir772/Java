import java.util.Scanner;

public class Reversetext {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("enter your text: ");
        String text = scanner.nextLine();
        
        String reversedText = "";
        for (int i = 0; i < text.length(); i++) {
            char loopChar = text.charAt(i);
            reversedText = loopChar + reversedText;
        }
        
        System.out.println(reversedText);
        scanner.close();
    }

}

/*
import java.util.Scanner;

public class ReverseMethod2 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("enter your text: ");
        String text = scanner.nextLine();
        
        // استخدام StringBuilder لعكس النص مباشرة (تشبه اختصار بايثون من حيث السرعة والسهولة)
        String reversedText = new StringBuilder(text).reverse().toString();
        
        System.out.println(reversedText);
        scanner.close();
    }
}*/