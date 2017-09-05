
PROFILES = nodeps alldeps

ALL: default dalston classpath

default:
	@./mvnw clean install

# is failing, it does not know artemis dependencies
angel:
	@./mvnw clean test -Dversion.org.springframework.boot=1.2.8.RELEASE -Dversion.org.springframework.cloud-spring-cloud-dependencies=Angel.SR6

dalston:
	@./mvnw clean test -Dversion.org.springframework.boot=1.5.6.RELEASE -Dversion.org.springframework.cloud-spring-cloud-dependencies=Dalston.SR3

classpath:
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		./mvnw -f opentracing-spring-cloud-starter -P $$profile clean test; \
	done

