# Spring Boot Backend Initialization

To initialize the Spring Boot backend in this directory, follow the instructions below.

> [!NOTE]
> We found **Java 21** installed on your system at:
> `C:\Users\MAHMO\.jdks\temurin-21.0.11`
>
> You can target **Java 21** with **Spring Boot 3.x**!

---

### Option 1: Using the Web UI (easiest)
1. Go to [Spring Initializr (start.spring.io)](https://start.spring.io/)
2. Choose your project settings:
   - **Project:** Maven or Gradle
   - **Language:** Java
   - **Spring Boot Version:** `3.3.x`
   - **Java Version:** `21`
   - **Dependencies:** Web, JPA, H2/MySQL, DevTools, etc.
3. Click **Generate** to download the ZIP file.
4. Extract the ZIP contents directly into this `Backend` folder.

---

### Option 2: Command Line (PowerShell)
You can use `curl.exe` to fetch a pre-configured project from the Spring Initializr API. 

Run these commands from inside the `Backend` directory:

```powershell
# For Spring Boot 3.x with Java 21
curl.exe -G https://start.spring.io/starter.zip `
  -d dependencies=web,data-jpa,h2 `
  -d javaVersion=21 `
  -d bootVersion=3.3.1 `
  -d type=maven-project `
  -d baseDir=backend-app `
  -o backend.zip

# Expand the zip and clean up
Expand-Archive -Path backend.zip -DestinationPath . -Force
Move-Item -Path .\backend-app\* -DestinationPath . -Force
Remove-Item -Path .\backend-app -Recurse -Force
Remove-Item -Path .\backend.zip -Force
```

If you are stuck on **Java 8** and need **Spring Boot 2.7.x**:
```powershell
curl.exe -G https://start.spring.io/starter.zip `
  -d dependencies=web,data-jpa,h2 `
  -d javaVersion=1.8 `
  -d bootVersion=2.7.18 `
  -d type=maven-project `
  -d baseDir=backend-app `
  -o backend.zip

# Expand the zip and clean up
Expand-Archive -Path backend.zip -DestinationPath . -Force
Move-Item -Path .\backend-app\* -DestinationPath . -Force
Remove-Item -Path .\backend-app -Recurse -Force
Remove-Item -Path .\backend.zip -Force
```
