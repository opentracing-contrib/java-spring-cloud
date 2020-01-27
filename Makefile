PROFILES = nodeps alldeps

ALL: default classpath

default:
	@./mvnw clean install

greenwich:
	@./mvnw clean test -Dversion.org.springframework.boot=2.1.5.RELEASE -Dversion.org.springframework.cloud-spring-cloud-dependencies=Greenwich.SR1

classpath:
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		./mvnw -f opentracing-spring-cloud-starter -P $$profile clean test || exit 1; \
	done
