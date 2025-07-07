package com.responsive.ai.sql_prompter.service;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class DatabaseSeedService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSeedService.class);
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    private final Random random = new Random();
    
    @PostConstruct
    public void seedDatabase() {
        try {
            log.info("Starting database seeding...");
            seedDepartments();
            seedEmployees();
            seedProjects();
            log.info("Database seeding completed successfully");
        } catch (Exception e) {
            log.error("Error seeding database", e);
        }
    }
    
    // ... [previous seed methods remain the same] ...
    
    private void seedDepartments() {
        if (mongoTemplate.collectionExists("departments") && 
            mongoTemplate.getCollection("departments").countDocuments() > 0) {
            log.info("Departments already exist, skipping...");
            return;
        }
        
        log.info("📊 Seeding departments...");
        List<Document> departments = Arrays.asList(
            createDepartment("ENG", "Engineering", 5000000, "San Francisco"),
            createDepartment("MKT", "Marketing", 2000000, "New York"),
            createDepartment("FIN", "Finance", 3000000, "Chicago"),
            createDepartment("HR", "Human Resources", 1000000, "Austin"),
            createDepartment("SALES", "Sales", 4000000, "Dallas")
        );
        
        mongoTemplate.dropCollection("departments");
        mongoTemplate.insert(departments, "departments");
    }
    
    private Document createDepartment(String code, String name, double budget, String location) {
        return new Document("code", code)
            .append("name", name)
            .append("budget", budget)
            .append("location", location)
            .append("active", true)
            .append("createdAt", new Date());
    }
    
    private void seedEmployees() {
        if (mongoTemplate.collectionExists("employees") && 
            mongoTemplate.getCollection("employees").countDocuments() > 0) {
            log.info("Employees already exist, skipping...");
            return;
        }
        
        log.info("👥 Seeding employees...");
        List<Document> departments = mongoTemplate.findAll(Document.class, "departments");
        if (departments.isEmpty()) {
            log.warn("No departments found, creating default department...");
            seedDepartments();
            departments = mongoTemplate.findAll(Document.class, "departments");
        }
        
        List<Document> employees = new ArrayList<>();
    
    // First create 5 recent hires (within last 30 days)
    for (int i = 1; i <= 5; i++) {
        Document department = departments.get(random.nextInt(departments.size()));
        Document employee = createEmployee(i, department);
        // Override join date to be within last 30 days
        LocalDate recentJoinDate = LocalDate.now().minusDays(random.nextInt(30));
        employee.put("joinDate", Date.from(recentJoinDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        employees.add(employee);
    }
    
    // Create remaining employees with random join dates
    for (int i = 6; i <= 50; i++) {
        Document department = departments.get(random.nextInt(departments.size()));
        employees.add(createEmployee(i, department));
    }
        
        mongoTemplate.dropCollection("employees");
        mongoTemplate.insert(employees, "employees");
    }
    
    private Document createEmployee(int id, Document department) {
        String[] firstNames = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Lisa", "James", "Jennifer"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        String[] domains = {"engineering.com", "techcorp.com", "devops.io", "cloudnine.tech"};
        String[] educationLevels = {"High School", "Associate's", "Bachelor's", "Master's", "PhD", "MBA"};
        String[] performanceRatings = {"Exceeds Expectations", "Meets Expectations", "Needs Improvement"};
        
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];
        String email = (firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + 
                      domains[random.nextInt(domains.length)]).toLowerCase();
        int age = 22 + random.nextInt(30);
        String gender = random.nextBoolean() ? "Male" : "Female";
        String education = educationLevels[random.nextInt(educationLevels.length)];
        String performance = performanceRatings[random.nextInt(performanceRatings.length)];
        
        // Generate a more realistic join date (last 5 years)
        LocalDate joinDate = LocalDate.now()
            .minusYears(random.nextInt(5))
            .minusMonths(random.nextInt(12))
            .minusDays(random.nextInt(30));
            
        // Calculate years of experience (1-10 years)
        int yearsOfExperience = 1 + random.nextInt(10);
        
        // Generate salary based on position and experience
        String position = getRandomPosition(department.getString("code"));
        double baseSalary = getBaseSalary(position);
        double experienceMultiplier = 1.0 + (yearsOfExperience * 0.05);
        double salary = Math.round((baseSalary * experienceMultiplier * (0.9 + random.nextDouble() * 0.2)) * 100) / 100.0;
        
        // Generate manager ID (null for some employees)
        String managerId = (id > 10 && random.nextDouble() > 0.3) ? 
                         "EMP" + String.format("%04d", 1 + random.nextInt(10)) : null;
        
        return new Document("employeeId", "EMP" + String.format("%04d", id))
            .append("firstName", firstName)
            .append("lastName", lastName)
            .append("email", email)
            .append("phone", generatePhoneNumber())
            .append("age", age)
            .append("gender", gender)
            .append("education", education)
            .append("department", department.get("code"))
            .append("position", position)
            .append("salary", salary)
            .append("joinDate", Date.from(joinDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .append("yearsOfExperience", yearsOfExperience)
            .append("isActive", random.nextDouble() > 0.1)
            .append("isFullTime", random.nextDouble() > 0.2) // 80% full-time
            .append("skills", getRandomSkills())
            .append("languages", getRandomLanguages())
            .append("certifications", getRandomCertifications())
            .append("performanceRating", performance)
            .append("managerId", managerId)
            .append("emergencyContact", createEmergencyContact())
            .append("address", createAddress(department.getString("location")))
            .append("lastPromotionDate", random.nextDouble() > 0.7 ? 
                Date.from(joinDate.plusMonths(random.nextInt(24)).atStartOfDay(ZoneId.systemDefault()).toInstant()) : null)
            .append("createdAt", new Date())
            .append("updatedAt", new Date());
    }
    
    private double getBaseSalary(String position) {
        if (position.contains("Manager") || position.contains("Director") || position.equals("CFO")) {
            return 80000 + random.nextInt(120000);
        } else if (position.contains("Senior") || position.contains("Lead")) {
            return 70000 + random.nextInt(80000);
        } else if (position.contains("Junior") || position.contains("Associate")) {
            return 45000 + random.nextInt(30000);
        } else {
            return 50000 + random.nextInt(50000);
        }
    }
    
    private String generatePhoneNumber() {
        return String.format("(%03d) %03d-%04d",
            200 + random.nextInt(800),
            100 + random.nextInt(900),
            1000 + random.nextInt(9000));
    }
    
    private Document createEmergencyContact() {
        String[] names = {"Alex Johnson", "Taylor Smith", "Jordan Williams", "Casey Brown", "Riley Garcia"};
        String[] relationships = {"Spouse", "Parent", "Sibling", "Friend", "Relative"};
        
        return new Document("name", names[random.nextInt(names.length)])
            .append("relationship", relationships[random.nextInt(relationships.length)])
            .append("phone", generatePhoneNumber());
    }
    
    private List<String> getRandomLanguages() {
        String[][] languageProficiency = {
            {"English", "Native"},
            {"Spanish", random.nextBoolean() ? "Fluent" : "Basic"},
            {"French", random.nextBoolean() ? "Fluent" : "Basic"},
            {"German", random.nextBoolean() ? "Fluent" : "Basic"},
            {"Mandarin", random.nextBoolean() ? "Fluent" : "Basic"}
        };
        
        List<String> languages = new ArrayList<>();
        // Always include English
        languages.add("English (Native)");
        
        // Add 1-3 additional languages
        int numLanguages = 1 + random.nextInt(3);
        Collections.shuffle(Arrays.asList(languageProficiency));
        
        for (int i = 0; i < Math.min(numLanguages, languageProficiency.length - 1); i++) {
            String[] lang = languageProficiency[i];
            if (!lang[0].equals("English")) {
                languages.add(String.format("%s (%s)", lang[0], lang[1]));
            }
        }
        
        return languages;
    }
    
    private List<String> getRandomCertifications() {
        String[] allCerts = {
            "AWS Certified Solutions Architect",
            "Google Cloud Professional",
            "Certified Scrum Master",
            "PMP",
            "CISSP",
            "CISCO CCNA",
            "Microsoft Certified: Azure Administrator",
            "Oracle Certified Professional",
            "Certified Data Professional",
            "ITIL Foundation"
        };
        
        List<String> certs = new ArrayList<>();
        int numCerts = random.nextInt(4); // 0-3 certifications
        Collections.shuffle(Arrays.asList(allCerts));
        
        for (int i = 0; i < numCerts; i++) {
            certs.add(allCerts[i]);
        }
        
        return certs;
    }
    
    private String getRandomPosition(String deptCode) {
        Map<String, List<String>> positions = Map.of(
            "ENG", List.of("Software Engineer", "Senior Developer", "Tech Lead", "Architect", "QA Engineer"),
            "MKT", List.of("Marketing Manager", "Content Writer", "SEO Specialist", "Social Media Manager"),
            "FIN", List.of("Accountant", "Financial Analyst", "CFO", "Controller"),
            "HR", List.of("HR Manager", "Recruiter", "HR Business Partner", "Training Specialist"),
            "SALES", List.of("Sales Executive", "Account Manager", "Sales Director", "Business Development")
        );
        List<String> deptPositions = positions.getOrDefault(deptCode, List.of("Associate", "Specialist"));
        return deptPositions.get(random.nextInt(deptPositions.size()));
    }
    
    private List<String> getRandomSkills() {
        List<String> allSkills = new ArrayList<>(Arrays.asList(
            "Java", "Python", "JavaScript", "SQL", "MongoDB", "Spring Boot", "React", "Node.js",
            "Project Management", "Agile", "Scrum", "Data Analysis", "Market Research", "SEO",
            "Financial Modeling", "Accounting", "Taxation", "Recruitment", "Employee Relations"
        ));
        Collections.shuffle(allSkills);
        return allSkills.subList(0, 3 + random.nextInt(3));
    }
    
    private Document createAddress(String city) {
        return new Document("street", (random.nextInt(9000) + 100) + " Main St")
            .append("city", city)
            .append("state", getState(city))
            .append("zipCode", String.format("%05d", random.nextInt(90000) + 10000))
            .append("country", "USA");
    }
    
    private String getState(String city) {
        Map<String, String> cityToState = Map.of(
            "San Francisco", "CA",
            "New York", "NY",
            "Chicago", "IL",
            "Austin", "TX",
            "Dallas", "TX"
        );
        return cityToState.getOrDefault(city, "CA");
    }
    
    private void seedProjects() {
        if (mongoTemplate.collectionExists("projects") && 
            mongoTemplate.getCollection("projects").countDocuments() > 0) {
            log.info("Projects already exist, skipping...");
            return;
        }
        
        log.info("Seeding projects...");
        List<Document> departments = mongoTemplate.findAll(Document.class, "departments");
        if (departments.isEmpty()) {
            log.warn("No departments found, creating default department...");
            seedDepartments();
            departments = mongoTemplate.findAll(Document.class, "departments");
        }
        
        List<Document> projects = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Document department = departments.get(random.nextInt(departments.size()));
            projects.add(createProject(i, department));
        }
        
        mongoTemplate.dropCollection("projects");
        mongoTemplate.insert(projects, "projects");
    }
    
    private Document createProject(int id, Document department) {
        String[] projectTypes = {"Web Application", "Mobile App", "API Development", "Data Migration", "Cloud Migration"};
        String[] statuses = {"Planning", "In Progress", "On Hold", "Completed", "Cancelled"};
        
        String projectName = department.getString("name") + " " + 
                           projectTypes[random.nextInt(projectTypes.length)] + " " + id;
                           
        Date startDate = new Date();
        Date endDate = Date.from(startDate.toInstant().plusSeconds(
            (30 + random.nextInt(330)) * 24 * 60 * 60));
            
        return new Document("projectId", "PRJ" + String.format("%03d", id))
            .append("name", projectName)
            .append("department", department.get("code"))
            .append("description", "Project for " + department.getString("name") + " department")
            .append("budget", 10000 + (random.nextDouble() * 90000))
            .append("startDate", startDate)
            .append("endDate", endDate)
            .append("status", statuses[random.nextInt(statuses.length)])
            .append("createdAt", new Date());
    }
}
