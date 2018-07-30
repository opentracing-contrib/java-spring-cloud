PROFILES = nodeps alldeps

ALL: default classpath

default:
	@./mvnw clean install

classpath:
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		./mvnw -f opentracing-spring-cloud-starter -P $$profile clean test || exit 1; \
	done
