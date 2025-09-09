FROM dcache/maven-java17-rpm-build


ADD . /dcache
WORKDIR /dcache
CMD [ "sleep", "99999"]
