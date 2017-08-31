
PROFILES = nodeps alldeps

TEST_ALL:
	@./mvnw clean install
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		./mvnw -f opentracing-spring-cloud-starter -P $$profile clean test; \
	done
