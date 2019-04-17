PROFILES = nodeps alldeps

ALL: default classpath

default:
	@./mvnw clean install

finchley:
	@./mvnw clean test -Dversion.org.springframework.boot=2.0.3.RELEASE -Dversion.org.springframework.cloud-spring-cloud-dependencies=Finchley.RELEASE

classpath:
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		./mvnw -f opentracing-spring-cloud-starter -P $$profile clean test || exit 1; \
	done
