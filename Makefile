
PROFILES = nodeps alldeps

ALL: default classpath

default:
	@./mvnw clean install

# is failing, it does not know artemis dependencies
angel:
	@./mvnw clean test -Dversion.org.springframework.boot=1.2.8.RELEASE -Dversion.org.springframework.cloud-spring-cloud-dependencies=Angel.SR6

edgware:
	@./mvnw clean test -Dversion.org.springframework.boot=1.5.9.RELEASE -Dversion.org.springframework.cloud-spring-cloud-dependencies=Edgware.RELEASE

classpath:
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		./mvnw -f opentracing-spring-cloud-starter -P $$profile clean test || exit 1; \
	done
