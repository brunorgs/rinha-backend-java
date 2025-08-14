FROM alpine:3.22.1
RUN apk add --no-cache libc6-compat
COPY build/native/nativeCompile/backend /app/rinha
RUN chmod +x /app/rinha
RUN ls -la /app/
WORKDIR /app
CMD ["./rinha", "--gc=G1 ", "-Xms90m ", "-Xmx90m"]