
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static final int MAX_DAYS_PER_EMPLOYEE = 5;
    private static final int MIN_EMPLOYEES_PER_SHIFT = 2;
    private static final int MAX_PER_SHIFT = 2;
    private static final List<String> DAYS = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );
    private static final List<Shift> SHIFTS = Arrays.asList(Shift.MORNING, Shift.AFTERNOON, Shift.EVENING);

    private static final Map<String, Employee> EMPLOYEES = new LinkedHashMap<>();
    private static final Schedule WEEK_SCHEDULE = new Schedule();
    private static boolean scheduleGenerated = false;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Scheduler scheduler = new Scheduler(WEEK_SCHEDULE, EMPLOYEES);

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    inputEmployees(sc);
                    scheduleGenerated = false;
                    break;
                case "2":
                    if (EMPLOYEES.isEmpty()) {
                        System.out.println("No employees found. Please enter employees first (option 1).");
                        break;
                    }

                    WEEK_SCHEDULE.clear();
                    EMPLOYEES.values().forEach(Employee::resetWorkCounters);
                    scheduler.generateWeeklySchedule();
                    scheduleGenerated = true;
                    System.out.println("Schedule generated.");
                    break;
                case "3":
                    if (!scheduleGenerated) {
                        System.out.println("No schedule generated yet. Choose option 2 to generate.");
                    } else {
                        printScheduleTable(WEEK_SCHEDULE);
                    }
                    break;
                case "4":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please pick 1-4.");
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=================================");
        System.out.println(" Employee Scheduling Application");
        System.out.println("=================================");
        System.out.println("1. Enter Employees and Preferences");
        System.out.println("2. Generate Weekly Schedule");
        System.out.println("3. View Weekly Schedule");
        System.out.println("4. Exit");
        System.out.println("=================================");
        System.out.print("Enter your choice: ");
    }

    private static void inputEmployees(Scanner sc) {
        System.out.println("\nEnter employee names (type 'done' when finished):");
        while (true) {
            System.out.print("Employee name: ");
            String name = sc.nextLine().trim();
            if (name.equalsIgnoreCase("done")) break;
            if (name.isEmpty()) {
                System.out.println("Name cannot be empty.");
                continue;
            }
            if (EMPLOYEES.containsKey(name)) {
                System.out.println("Employee already exists. Updating their preferences.");
            }

            Employee emp = EMPLOYEES.getOrDefault(name, new Employee(name));

            System.out.println("Enter preferred shift per day for " + name +
                    " as one of: morning / afternoon / evening / none");
            for (String day : DAYS) {
                while (true) {
                    System.out.printf("  %s preference: ", day);
                    String pref = sc.nextLine().trim().toLowerCase(Locale.ROOT);
                    Shift shift = parseShift(pref);
                    if (shift != null || pref.equals("none") || pref.isEmpty()) {
                        if (shift == null) {

                            emp.preferredShiftsByDay.remove(day);
                        } else {
                            emp.preferredShiftsByDay.put(day, shift);
                        }
                        break;
                    } else {
                        System.out.println("Please enter 'morning', 'afternoon', 'evening', or 'none'.");
                    }
                }
            }

            EMPLOYEES.put(name, emp);
            System.out.println("Saved preferences for: " + name + "\n");
        }

        System.out.println("Employee entry complete. Total employees: " + EMPLOYEES.size());
    }

    private static Shift parseShift(String s) {
        switch (s) {
            case "morning": return Shift.MORNING;
            case "afternoon": return Shift.AFTERNOON;
            case "evening": return Shift.EVENING;
            default: return null;
        }
    }

    private static void printScheduleTable(Schedule schedule) {

        Map<String, Map<Shift, String>> table = new LinkedHashMap<>();
        int dayColWidth = "Day".length();
        int morningColWidth = "Morning".length();
        int afternoonColWidth = "Afternoon".length();
        int eveningColWidth = "Evening".length();

        for (String day : DAYS) {
            Map<Shift, List<String>> dayMap = schedule.schedule.getOrDefault(day, new EnumMap<>(Shift.class));
            Map<Shift, String> row = new EnumMap<>(Shift.class);
            for (Shift sh : SHIFTS) {
                List<String> names = dayMap.getOrDefault(sh, Collections.emptyList());
                String joined = String.join(", ", names);
                row.put(sh, joined);
                morningColWidth = Math.max(morningColWidth, (sh == Shift.MORNING ? joined.length() : morningColWidth));
                afternoonColWidth = Math.max(afternoonColWidth, (sh == Shift.AFTERNOON ? joined.length() : afternoonColWidth));
                eveningColWidth = Math.max(eveningColWidth, (sh == Shift.EVENING ? joined.length() : eveningColWidth));
            }
            table.put(day, row);
            dayColWidth = Math.max(dayColWidth, day.length());
        }

        // Header
        String header = padRight("Day", dayColWidth) + " | "
                + padRight("Morning", morningColWidth) + " | "
                + padRight("Afternoon", afternoonColWidth) + " | "
                + padRight("Evening", eveningColWidth);
        String sep = repeat("-", header.length());

        System.out.println("\n" + header);
        System.out.println(sep);
        for (String day : DAYS) {
            Map<Shift, String> row = table.getOrDefault(day, new EnumMap<>(Shift.class));
            String line = padRight(day, dayColWidth) + " | "
                    + padRight(row.getOrDefault(Shift.MORNING, ""), morningColWidth) + " | "
                    + padRight(row.getOrDefault(Shift.AFTERNOON, ""), afternoonColWidth) + " | "
                    + padRight(row.getOrDefault(Shift.EVENING, ""), eveningColWidth);
            System.out.println(line);
        }

        // Optional warnings if any shift is still under-staffed
        List<String> warnings = new ArrayList<>();
        for (String day : DAYS) {
            Map<Shift, List<String>> dayMap = schedule.schedule.getOrDefault(day, new EnumMap<>(Shift.class));
            for (Shift sh : SHIFTS) {
                int size = dayMap.getOrDefault(sh, Collections.emptyList()).size();
                if (size < MIN_EMPLOYEES_PER_SHIFT) {
                    warnings.add(String.format("%s %s shift has only %d employee(s).",
                            day, sh.display(), size));
                }
            }
        }
        if (!warnings.isEmpty()) {
            System.out.println("\n⚠ Warnings:");
            warnings.forEach(w -> System.out.println(" - " + w));
        }
        System.out.println();
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(n * Math.max(1, s.length()));
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }


    enum Shift {
        MORNING("Morning"),
        AFTERNOON("Afternoon"),
        EVENING("Evening");

        private final String label;
        Shift(String label) { this.label = label; }
        public String display() { return label; }
    }

    static class Employee {
        final String name;
        final Map<String, Shift> preferredShiftsByDay = new LinkedHashMap<>();
        int daysAssigned = 0;
        final Set<String> daysWorked = new HashSet<>();

        Employee(String name) {
            this.name = name;
        }

        void resetWorkCounters() {
            daysAssigned = 0;
            daysWorked.clear();
        }

        boolean canWorkDay(String day) {
            return !daysWorked.contains(day) && daysAssigned < MAX_DAYS_PER_EMPLOYEE;
        }

        void assign(String day) {
            daysWorked.add(day);
            daysAssigned++;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class Schedule {

        final Map<String, Map<Shift, List<String>>> schedule = new LinkedHashMap<>();
        Schedule() {
            for (String day : DAYS) {
                schedule.put(day, new EnumMap<>(Shift.class));
                for (Shift sh : SHIFTS) {
                    schedule.get(day).put(sh, new ArrayList<>());
                }
            }
        }
        void clear() {
            schedule.values().forEach(map -> map.values().forEach(List::clear));
        }
        boolean hasCapacity(String day, Shift shift) {
            return schedule.get(day).get(shift).size() < MAX_PER_SHIFT;
        }
        void add(String day, Shift shift, Employee e) {
            schedule.get(day).get(shift).add(e.name);
        }
        int size(String day, Shift shift) {
            return schedule.get(day).get(shift).size();
        }
    }

    static class Scheduler {
        private final Schedule schedule;
        private final Map<String, Employee> employees;
        private final Random rng = new Random();

        Scheduler(Schedule schedule, Map<String, Employee> employees) {
            this.schedule = schedule;
            this.employees = employees;
        }

        void generateWeeklySchedule() {

            List<Employee> carryover = new ArrayList<>();

            for (int d = 0; d < DAYS.size(); d++) {
                String day = DAYS.get(d);

                List<Employee> todaysCandidates = employees.values().stream()
                        .filter(e -> e.preferredShiftsByDay.containsKey(day))
                        .collect(Collectors.toList());

                Collections.shuffle(todaysCandidates, rng);

                for (Employee e : todaysCandidates) {
                    Shift preferred = e.preferredShiftsByDay.get(day);
                    attemptAssignWithConflictResolution(e, day, preferred, carryover, d);
                }

                if (!carryover.isEmpty()) {

                    Collections.shuffle(carryover, rng);
                    Iterator<Employee> it = carryover.iterator();
                    while (it.hasNext()) {
                        Employee e = it.next();
                        if (!e.canWorkDay(day)) continue;
                        // Try any available shift today (no preference for carryover)
                        boolean placed = tryAnyShiftToday(e, day);
                        if (placed) it.remove();
                    }
                }

                ensureMinimumStaffing(day);

            }

        }

        private void attemptAssignWithConflictResolution(Employee e, String day, Shift preferred,
                                                         List<Employee> carryover, int dayIndex) {
            if (!e.canWorkDay(day)) {
                pushToNextDayIfPossible(e, carryover, dayIndex);
                return;
            }

            if (preferred != null && schedule.hasCapacity(day, preferred)) {
                schedule.add(day, preferred, e);
                e.assign(day);
                return;
            }

            for (Shift alt : SHIFTS) {
                if (alt == preferred) continue;
                if (schedule.hasCapacity(day, alt) && e.canWorkDay(day)) {
                    schedule.add(day, alt, e);
                    e.assign(day);
                    return;
                }
            }

            pushToNextDayIfPossible(e, carryover, dayIndex);
        }

        private void pushToNextDayIfPossible(Employee e, List<Employee> carryover, int dayIndex) {
            if (dayIndex < DAYS.size() - 1 && e.daysAssigned < MAX_DAYS_PER_EMPLOYEE) {
                carryover.add(e);
            }
        }

        private boolean tryAnyShiftToday(Employee e, String day) {
            if (!e.canWorkDay(day)) return false;
            List<Shift> shuffled = new ArrayList<>(SHIFTS);
            Collections.shuffle(shuffled, rng);
            for (Shift sh : shuffled) {
                if (schedule.hasCapacity(day, sh)) {
                    schedule.add(day, sh, e);
                    e.assign(day);
                    return true;
                }
            }
            return false;
        }

        private void ensureMinimumStaffing(String day) {
            for (Shift sh : SHIFTS) {
                while (schedule.size(day, sh) < MIN_EMPLOYEES_PER_SHIFT) {
                    // pick a random eligible employee
                    List<Employee> eligible = employees.values().stream()
                            .filter(e -> e.canWorkDay(day))
                            .filter(e -> !schedule.schedule.get(day).values()
                                    .stream()
                                    .flatMap(List::stream)
                                    .collect(Collectors.toSet())
                                    .contains(e.name))
                            .collect(Collectors.toList());

                    if (eligible.isEmpty()) break;

                    Employee pick = eligible.get(rng.nextInt(eligible.size()));
                    if (schedule.hasCapacity(day, sh)) {
                        schedule.add(day, sh, pick);
                        pick.assign(day);
                    } else {
                        break;
                    }
                }
            }
        }
    }
}
