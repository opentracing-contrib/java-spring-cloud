
PROFILES = nodeps feign

ALL:
	@for profile in $(PROFILES) ; do \
		echo "---> Executing profile $$profile"; \
		../mvnw clean test -P $$profile; \
	done
