start "CreatorSpace Frontend" cmd /k "npm run dev --prefix frontend"
start "CreatorSpace Backend" cmd /k "mvn -f backend/pom.xml spring-boot:run"
