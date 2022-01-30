runserver:
	./gradlew bootJar
	docker-compose up --build -d
	@echo "Loading mongo data"
	docker cp ./db/mongo_data/data workffice-mongo-db:/tmp/data/
	docker exec -it workffice-mongo-db mongorestore --host=localhost --port=27017 /tmp/data

stopserver:
	@echo "Dumping authentication mysql data 📜"
	docker exec -it mysql-authentication-db mysqldump -h localhost -u root --password=1234 workffice > ./db/authentication_data.sql
	@echo "Dumping backoffice mysql data 📜"
	docker exec -it mysql-backoffice-db mysqldump -h localhost -u root --password=1234 workffice > ./db/backoffice_data.sql
	@echo "Dumping booking mysql data 📜"
	docker exec -it mysql-booking-db mysqldump -h localhost -u root --password=1234 workffice > ./db/booking_data.sql
	@echo "Dumping mongo mysql data 📜"
	docker exec -it workffice-mongo-db mongodump --db workffice --out /tmp/data
	rm -r ./db/mongo_data
	mkdir ./db/mongo_data
	docker cp workffice-mongo-db:/tmp/data ./db/mongo_data
	@echo "Finish dumping mysql and mongo data ✔️"
	docker-compose down

log-server:
	docker logs workffice-server -f

setup-test-dbs:
	docker-compose -f docker-compose-test.yml up --build -d

teardown-test-dbs:
	docker-compose -f docker-compose-test.yml down

test:
	docker-compose -f docker-compose-test.yml up --build -d
	./gradlew test
	docker-compose -f docker-compose-test.yml down

generate-coverage-report:
	docker-compose -f docker-compose-test.yml up --build -d
	sleep 10
	./gradlew test
	./gradlew jacocoTestReport
	docker-compose -f docker-compose-test.yml down

load-mongo-data:
	docker cp ./db/mongo_data/data workffice-mongo-db:/tmp/data/
	docker exec -it workffice-mongo-db mongorestore --host=localhost --port=27017 /tmp/data
