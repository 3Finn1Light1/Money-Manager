package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Главный класс приложения "MoneyTracker".
 * Запускает программу для учета расходов.
 */
public class Money {
    public static void main(String[] args) {
        MoneyTracker tracker = new MoneyTracker();
        tracker.startApplication();
    }
}

/**
 * Класс MoneyTracker управляет работой приложения.
 * Обеспечивает взаимодействие с пользователем и обработку основных функций:
 * добавление, отображение, удаление расходов и экспорт в Excel.
 */
class MoneyTracker {
    private final ExpenseManager expenseManager = new ExpenseManager();
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Запускает работу приложения: отображает главное меню и обрабатывает пользовательский ввод.
     */
    public void startApplication() {
        expenseManager.loadExpensesFromFile(); // Загрузка данных из файла при запуске

        boolean running = true;
        while (running) {
            System.out.println("\nМеню учета расходов:");
            System.out.println("1. Добавить расход");
            System.out.println("2. Показать статистику");
            System.out.println("3. Удалить расход");
            System.out.println("4. Экспорт в Excel");
            System.out.println("5. Выход");
            System.out.print("Выберите опцию: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> expenseManager.addExpense(scanner);
                case 2 -> expenseManager.showStatistics(scanner);
                case 3 -> expenseManager.deleteExpense(scanner);
                case 4 -> new ExcelExporter().export(expenseManager.getExpenses());
                case 5 -> running = false;
                default -> System.out.println("Неверный ввод. Попробуйте снова.");
            }
        }

        expenseManager.saveExpensesToFile(); // Сохранение данных в файл при завершении
        System.out.println("До свидания!");
    }
}

/**
 * Класс ExpenseManager управляет расходами.
 * Обеспечивает функции добавления, просмотра, удаления и сохранения/загрузки расходов.
 */
class ExpenseManager {
    private static final String[] CATEGORIES = {
            "Еда", "Транспорт", "Развлечения", "Здоровье", "Образование",
            "Коммунальные услуги", "Шопинг", "Путешествия", "Аренда", "Разное"
    };
    private static final DateTimeFormatter EUROPEAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String DATA_FILE = "expenses.dat";

    private final List<Expense> expenses = new ArrayList<>();

    /**
     * Добавляет новый расход в список.
     *
     * @param scanner объект Scanner для ввода данных пользователем
     */
    public void addExpense(Scanner scanner) {
        System.out.println("Введите 0 для выхода в меню.");
        System.out.print("Введите сумму: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        if (amount == 0) {
            System.out.println("Выход в главное меню.");
            return;
        }

        System.out.println("Выберите категорию:");
        for (int i = 0; i < CATEGORIES.length; i++) {
            System.out.printf("%d. %s\n", i + 1, CATEGORIES[i]);
        }
        System.out.println("0. Выход в меню.");
        int categoryIndex = scanner.nextInt() - 1;
        scanner.nextLine();

        if (categoryIndex == -1) {
            System.out.println("Выход в главное меню.");
            return;
        }

        if (categoryIndex < 0 || categoryIndex >= CATEGORIES.length) {
            System.out.println("Неверная категория.");
            return;
        }

        LocalDate date = parseDateInput(scanner);
        if (date == null) return;

        expenses.add(new Expense(amount, CATEGORIES[categoryIndex], date));
        System.out.println("Расход успешно добавлен!");
    }

    /**
     * Отображает статистику расходов за выбранный месяц.
     *
     * @param scanner объект Scanner для ввода данных пользователем
     */
    public void showStatistics(Scanner scanner) {
        System.out.println("Введите 0 для выхода в меню.");
        System.out.print("Введите месяц и год (ММ.ГГГГ) для статистики: ");
        String input = scanner.nextLine();

        if (input.equals("0")) {
            System.out.println("Выход в главное меню.");
            return;
        }

        YearMonth yearMonth = parseYearMonthInput(input);
        if (yearMonth == null) return;

        Map<String, Double> categoryTotals = new HashMap<>();
        double totalExpenses = 0;

        for (String category : CATEGORIES) {
            categoryTotals.put(category, 0.0);
        }

        for (Expense expense : expenses) {
            if (YearMonth.from(expense.getDate()).equals(yearMonth)) {
                categoryTotals.put(expense.getCategory(), categoryTotals.get(expense.getCategory()) + expense.getAmount());
                totalExpenses += expense.getAmount();
            }
        }

        if (totalExpenses == 0) {
            System.out.println("За указанный месяц расходы отсутствуют.");
            return;
        }

        System.out.printf("\nОбщие расходы за %s: %.2f\n", yearMonth, totalExpenses);
        for (String category : CATEGORIES) {
            double categoryTotal = categoryTotals.get(category);
            double percentage = (categoryTotal / totalExpenses) * 100;
            System.out.printf("%s: %.2f (%.2f%%)\n", category, categoryTotal, percentage);
        }
    }

    /**
     * Удаляет расходы за определенный месяц.
     *
     * @param scanner объект Scanner для ввода данных пользователем
     */
    public void deleteExpense(Scanner scanner) {
        System.out.println("Введите 0 для выхода в меню.");
        System.out.print("Введите месяц и год (ММ.ГГГГ) для удаления расходов: ");
        String input = scanner.nextLine();

        if (input.equals("0")) {
            System.out.println("Выход в главное меню.");
            return;
        }

        YearMonth targetMonth = parseYearMonthInput(input);
        if (targetMonth == null) return;

        expenses.removeIf(expense -> YearMonth.from(expense.getDate()).equals(targetMonth));
        System.out.println("Расходы за указанный месяц успешно удалены.");
    }

    /**
     * Возвращает текущий список расходов.
     *
     * @return список объектов Expense
     */
    public List<Expense> getExpenses() {
        return expenses;
    }

    /**
     * Сохраняет текущий список расходов в файл.
     * Используется формат сериализации объектов Java (ObjectOutputStream).
     */
    public void saveExpensesToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(expenses);
            System.out.println("Данные успешно сохранены.");
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении данных: " + e.getMessage());
        }
    }

    /**
     * Загружает список расходов из файла.
     * Если файл не существует, метод ничего не делает.
     */
    public void loadExpensesFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Expense> loadedExpenses = (List<Expense>) ois.readObject();
            expenses.clear();
            expenses.addAll(loadedExpenses);
            System.out.println("Данные успешно загружены.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка при загрузке данных: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает ввод даты пользователем.
     * Запрашивает дату в формате "ДД.ММ.ГГГГ" и возвращает объект LocalDate.
     *
     * @param scanner объект Scanner для ввода данных пользователем
     * @return объект LocalDate или null, если пользователь отменил ввод
     */
    private LocalDate parseDateInput(Scanner scanner) {
        while (true) {
            System.out.print("Введите дату (ДД.ММ.ГГГГ): ");
            String input = scanner.nextLine();

            if (input.equals("0")) {
                System.out.println("Выход в главное меню.");
                return null;
            }

            try {
                return LocalDate.parse(input, EUROPEAN_DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Неверный формат даты или несуществующая дата. Попробуйте снова.");
            }
        }
    }

    /**
     * Обрабатывает ввод месяца и года пользователем.
     * Запрашивает данные в формате "ММ.ГГГГ" и возвращает объект YearMonth.
     *
     * @param input строка, введенная пользователем
     * @return объект YearMonth или null, если ввод был некорректным
     */
    private YearMonth parseYearMonthInput(String input) {
        try {
            return YearMonth.parse(input, DateTimeFormatter.ofPattern("MM.yyyy"));
        } catch (DateTimeParseException e) {
            System.out.println("Неверный формат месяца и года. Попробуйте снова.");
            return null;
        }
    }
}

/**
 * Класс ExcelExporter отвечает за экспорт расходов в файл Excel.
 * Использует библиотеку Apache POI для работы с форматом XLSX.
 */
class ExcelExporter {
    /**
     * Экспортирует список расходов в Excel-файл.
     * Создает отдельный лист для каждого месяца с данными о расходах.
     *
     * @param expenses список расходов для экспорта
     */
    public void export(List<Expense> expenses) {
        Workbook workbook = new XSSFWorkbook();

        // Группировка расходов по месяцам
        Map<YearMonth, List<Expense>> expensesByMonth = new HashMap<>();
        for (Expense expense : expenses) {
            YearMonth month = YearMonth.from(expense.getDate());
            expensesByMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(expense);
        }

        // Создание листов для каждого месяца
        for (YearMonth month : expensesByMonth.keySet()) {
            Sheet sheet = workbook.createSheet(month.toString());
            createHeaderRow(sheet);
            fillSheetWithData(sheet, expensesByMonth.get(month));
        }


        // Запись данных в файл
        try (FileOutputStream fos = new FileOutputStream("expenses.xlsx")) {
            workbook.write(fos);
            System.out.println("Данные успешно экспортированы в файл expenses.xlsx.");
        } catch (IOException e) {
            System.out.println("Ошибка экспорта данных: " + e.getMessage());
        }
    }

    /**
     * Создает строку заголовков для таблицы в Excel.
     *
     * @param sheet объект Sheet, в который добавляются заголовки
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Дата");
        headerRow.createCell(1).setCellValue("Категория");
        headerRow.createCell(2).setCellValue("Сумма");
    }

    /**
     * Заполняет таблицу данными о расходах.
     *
     * @param sheet    объект Sheet, в который добавляются данные
     * @param expenses список расходов для записи
     */
    private void fillSheetWithData(Sheet sheet, List<Expense> expenses) {
        int rowNum = 1;
        for (Expense expense : expenses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(expense.getDate().toString());
            row.createCell(1).setCellValue(expense.getCategory());
            row.createCell(2).setCellValue(expense.getAmount());
        }
    }
}

/**
 * Класс Expense представляет отдельный расход.
 * Содержит сумму, категорию и дату расхода.
 */
class Expense implements Serializable {
    private static final long serialVersionUID = 1L;
    private final double amount;
    private final String category;
    private final LocalDate date;

    /**
     * Конструктор для создания нового объекта расхода.
     *
     * @param amount   сумма расхода
     * @param category категория расхода
     * @param date     дата расхода
     */
    public Expense(double amount, String category, LocalDate date) {
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    /**
     * Возвращает сумму расхода.
     *
     * @return сумма расхода
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Возвращает категорию расхода.
     *
     * @return категория расхода
     */
    public String getCategory() {
        return category;
    }

    /**
     * Возвращает дату расхода.
     *
     * @return дата расхода
     */
    public LocalDate getDate() {
        return date;
    }
}
