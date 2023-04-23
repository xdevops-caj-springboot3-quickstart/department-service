# Department service

## How to register as a Eureka client

### Import dependencies

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Enable discovery client

Add `@EnableDiscoveryClient` annotation to the Spring Boot application.

### Configure Eureka connections

```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

## Run application

```bash
mvn spring-boot:run
```

or run the Spring Boot application directly.

## Check registered instance in Eureka dashboard

Access <http://localhost:8761/> in browser.

Check the registering services.

## Externalize the application configuration via the config server
### Move the application configuration to the config server

Move the application configuration from `application.yaml` to the `config/department-service.yaml` in the config server.

Need keep the `spring.applicaiton.name` in the original application.yaml.

Example:
```yaml
server:
  port: 8081

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

### Configure as a config client

Import dependencies:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

### Configure config server connections

```yaml
spring:
  config:
    import: "configserver:http://localhost:8088"
```

The <http://localhost:8088> is the config server uri.

## Enable distributed tracing via Zipkin

### Import dependencies

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
  <groupId>io.zipkin.reporter2</groupId>
  <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### Configure Zipkin connections

By default the distributed tracing data will be published to the `localhost:9411`.

### Publishing all tracing data to the Zipkin

Change sampling percentage as 100% (by default it is 10%).

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
```

Ensure already included the `spring-boot-starter-actuator` dependency.

## Build REST APIs

### Build APIs
Build REST APIs via controller-repository pattern. 

The repository writes/reads data in a memory list for demo only.

Use Lombok to simplify the Java bean and Logger codes.

### Test APIs
Use HTTPie for API testing.


### Add a customer

Request:
```bash
http POST :8081/departments id=1 name="Fiance"
```

Response:
```json
{
    "employees": [],
    "id": 1,
    "name": "Fiance"
}
```

Add another customer.

Request:
```bash
http POST :8081/departments id=2 name="Marketing"
```

Response:
```json
{
  "employees": [],
  "id": 2,
  "name": "Marketing"
}
```


### Find a customer by id

Request:
```bash
http GET :8081/departments/1
```

Response:
```json
{
    "employees": [],
    "id": 1,
    "name": "Fiance"
}
```

### Find all customers

Request:
```bash
http GET :8081/departments
```

Response:
```json
[
  {
    "employees": [],
    "id": 1,
    "name": "Fiance"
  },
  {
    "employees": [],
    "id": 2,
    "name": "Marketing"
  }
]
```

## Check the tracing data in Zipkin

Check the tracing data of department-service in Zipkin dashboard. 


## Call other Microservices

Call REST APIs of other Micorservices via WebFlux (Reactive and declarative).

### Import Spring Reactive Web dependencies

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>```
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### Create a declarative HTTP interface

Create `EmployeeClient` interface with `@HttpExchange` under `client` package.

```java
@HttpExchange("/employees")
public interface EmployeeClient {
    @GetExchange("/departments/{departmentId}")
    List<Employee> findByDepartmentId(@PathVariable Long departmentId);
}     
```

### Configure the declarative HTTP interface


Create `WebClientConfig` class under `config` package. 

```java
@Configuration
public class WebClientConfig {

    /**
     * Load balanced http exchange
     */
    @Autowired
    private LoadBalancedExchangeFilterFunction filterFunction;

    @Bean
    public EmployeeClient employeeClient() {
        /**
         * To create a proxy using the provided factory, besides the HTTP interface,
         * we'll also require an instance of a reactive web client
         */
        WebClient webClient = WebClient.builder()
                .baseUrl("http://employee-service")
                .filter(filterFunction)
                .build();

        /**
         * Spring framework provides us with a HttpServiceProxyFactory that
         * we can use to generate a client proxy for our HTTP interface:
         */
        return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient))
                .build()
                .createClient(EmployeeClient.class);
    }
}
```


### Use the HTTP interface to call other Microservices

```java
@RestController
@RequestMapping("/departments")
@Slf4j
public class DepartmentController {
    private final DepartmentRepository departmentRepository;
    private final EmployeeClient employeeClient;

    public DepartmentController(DepartmentRepository departmentRepository, EmployeeClient employeeClient) {
        this.departmentRepository = departmentRepository;
        this.employeeClient = employeeClient;
    }
    
    // omit other codes

    @GetMapping("/with-employees")
    public List<Department> findAllWithEmployees() {
        log.info("Department with employees find");
        List<Department> departments = departmentRepository.findAll();
        departments.forEach(department ->
                department.setEmployees(employeeClient.findByDepartmentId(department.getId())));
        return departments;
    }
}
```


### API Testing

Add departments firstly:

```bash
http POST :8081/departments id=1 name="Fiance"
http POST :8081/departments id=2 name="Marketing"
```

Find all departments with employees (call REST APIs of employee-service):

Request:

```bash
http GET :8081/departments/with-employees
```

Response:
```json
[
    {
        "employees": [
            {
                "age": 28,
                "departmentId": 1,
                "id": 1,
                "name": "William",
                "position": "Manager"
            },
            {
                "age": 30,
                "departmentId": 1,
                "id": 2,
                "name": "John",
                "position": "Business Consultant"
            }
        ],
        "id": 1,
        "name": "Fiance"
    },
    {
        "employees": [
            {
                "age": 32,
                "departmentId": 2,
                "id": 3,
                "name": "Tommy",
                "position": "Sales"
            }
        ],
        "id": 2,
        "name": "Marketing"
    }
]
```





## References

- https://cloud.spring.io/spring-cloud-netflix/reference/html/#service-discovery-eureka-clients
- https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_spring_cloud_config_client
- [Declarative REST Clients with Spring Framework 6](https://medium.com/digitalfrontiers/declarative-rest-clients-with-spring-framework-6-c671be1dfee)
- [Implementing declarative HTTP calls using the @HttpExchange annotation](https://www.springcloud.io/post/2023-02/spring-httpexchange/#gsc.tab=0)