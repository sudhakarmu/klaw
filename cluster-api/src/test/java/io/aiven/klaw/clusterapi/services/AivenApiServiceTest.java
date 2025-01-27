package io.aiven.klaw.clusterapi.services;

import static io.aiven.klaw.clusterapi.services.AivenApiService.OBJECT_MAPPER;
import static io.aiven.klaw.clusterapi.services.AivenApiService.PROJECT_NAME;
import static io.aiven.klaw.clusterapi.services.AivenApiService.SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.aiven.klaw.clusterapi.UtilMethods;
import io.aiven.klaw.clusterapi.models.AivenAclResponse;
import io.aiven.klaw.clusterapi.models.ClusterAclRequest;
import io.aiven.klaw.clusterapi.models.enums.ApiResultStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
public class AivenApiServiceTest {

  public static final String TESTUSER = "testuser";
  public static final String SERVICE_ACLS_ENDPOINT =
      "https://api.aiven.io/v1/project/projectName/service/serviceName/user/userName";
  AivenApiService aivenApiService;
  @Mock RestTemplate restTemplate;

  private UtilMethods utilMethods;

  private static final String ACLS_BASE_URL = "https://api.aiven.io/v1/project/";

  @BeforeEach
  public void setUp() {
    aivenApiService = new AivenApiService();
    ReflectionTestUtils.setField(aivenApiService, "restTemplate", restTemplate);
    ReflectionTestUtils.setField(
        aivenApiService,
        "addAclsApiEndpoint",
        "https://api.aiven.io/v1/project/projectName/service/serviceName/acl");
    ReflectionTestUtils.setField(
        aivenApiService,
        "listAclsApiEndpoint",
        "https://api.aiven.io/v1/project/projectName/service/serviceName/acl");
    ReflectionTestUtils.setField(
        aivenApiService,
        "deleteAclsApiEndpoint",
        "https://api.aiven.io/v1/project/projectName/service/serviceName/acl/aclId");
    ReflectionTestUtils.setField(
        aivenApiService, "serviceAccountApiEndpoint", SERVICE_ACLS_ENDPOINT);
    ReflectionTestUtils.setField(
        aivenApiService,
        "addServiceAccountApiEndpoint",
        "https://api.aiven.io/v1/project/projectName/service/serviceName/user");
    ReflectionTestUtils.setField(
        aivenApiService,
        "serviceDetailsApiEndpoint",
        "https://api.aiven.io/v1/project/projectName/service/serviceName");
    ReflectionTestUtils.setField(aivenApiService, "clusterAccessToken", "testtoken");
    ReflectionTestUtils.setField(aivenApiService, "restTemplate", restTemplate);
    utilMethods = new UtilMethods();
  }

  // Create Acls (adds service account)
  @Test
  public void createAclsServiceAccountDoesNotExist() throws Exception {
    ClusterAclRequest clusterAclRequest = utilMethods.getAivenAclRequest("Producer");
    String createAclsUri =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/acl";

    AivenAclResponse aivenAclResponse = utilMethods.getAivenAclResponse();
    String aivenAclResponseString = OBJECT_MAPPER.writeValueAsString(aivenAclResponse);

    // create acl stubs
    ResponseEntity<String> responseEntity =
        new ResponseEntity<>(aivenAclResponseString, HttpStatus.OK);
    when(restTemplate.postForEntity(eq(createAclsUri), any(), eq(String.class)))
        .thenReturn(responseEntity);

    // get service account stubs
    String getServiceAccountUri =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/user/"
            + clusterAclRequest.getUsername();
    // no record exists for this user, throw error by api
    when(restTemplate.exchange(
            eq(getServiceAccountUri),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Object>) any()))
        .thenThrow(new RuntimeException("No user found"));

    // create service account stubs
    String createServiceAccountUri =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/user";
    ResponseEntity<String> responseEntityAddAccount =
        new ResponseEntity<>("success", HttpStatus.OK);
    when(restTemplate.postForEntity(eq(createServiceAccountUri), any(), eq(String.class)))
        .thenReturn(responseEntityAddAccount);

    Map<String, String> response = aivenApiService.createAcls(clusterAclRequest);
    assertThat(response.get("result")).isEqualTo(ApiResultStatus.SUCCESS.value);
    assertThat(response.get("aivenaclid")).isEqualTo("testid");
  }

  // Create Acls (service account already exists)
  @Test
  public void createAclsServiceAccountExists() throws Exception {
    ClusterAclRequest clusterAclRequest = utilMethods.getAivenAclRequest("Producer");
    String createAclsUri =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/acl";

    AivenAclResponse aivenAclResponse = utilMethods.getAivenAclResponse();
    String aivenAclResponseString = OBJECT_MAPPER.writeValueAsString(aivenAclResponse);

    // create acl stubs
    ResponseEntity<String> responseEntity =
        new ResponseEntity<>(aivenAclResponseString, HttpStatus.OK);
    when(restTemplate.postForEntity(eq(createAclsUri), any(), eq(String.class)))
        .thenReturn(responseEntity);

    // get service account stubs
    String getServiceAccountUri =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/user/"
            + clusterAclRequest.getUsername();
    Map<String, Map<String, String>> serviceAccountResponse = new HashMap<>();
    Map<String, String> userNameMap = new HashMap<>();
    userNameMap.put("username", TESTUSER);
    serviceAccountResponse.put("user", userNameMap);
    ResponseEntity<Map<String, Map<String, String>>> responseEntityServiceAccount =
        new ResponseEntity<>(serviceAccountResponse, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(getServiceAccountUri),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Map<String, Map<String, String>>>) any()))
        .thenReturn(responseEntityServiceAccount);

    Map<String, String> response = aivenApiService.createAcls(clusterAclRequest);
    assertThat(response.get("result")).isEqualTo(ApiResultStatus.SUCCESS.value);
    assertThat(response.get("aivenaclid")).isEqualTo(aivenAclResponse.getAcl()[0].getId());
  }

  // Create Acls fails with acl already exists
  @Test
  public void createAclsServiceAccountExistsFailure() {
    ClusterAclRequest clusterAclRequest = utilMethods.getAivenAclRequest("Producer");
    String createAclsUri =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/acl";

    // create acl stubs throw error
    when(restTemplate.postForEntity(eq(createAclsUri), any(), eq(String.class)))
        .thenThrow(new RuntimeException("Acl ID already exists"));

    Map<String, String> response = aivenApiService.createAcls(clusterAclRequest);
    assertThat(response.get("result")).contains("Failure");
  }

  // Get service accounts
  @Test
  public void getServiceAccounts() {
    // get service account stubs
    String getServiceAccountUri = ACLS_BASE_URL + "testproject" + "/service/" + "testservice";

    Map<String, Map<String, Object>> serviceAccountsResponse = new HashMap<>();
    Map<String, Object> userNameMap = new HashMap<>();
    ArrayList<HashMap<String, Object>> userList = new ArrayList<>();
    HashMap<String, Object> userNameMapObj1 = new HashMap<>();
    userNameMapObj1.put("username", "user1");

    HashMap<String, Object> userNameMapObj2 = new HashMap<>();
    userNameMapObj2.put("username", "user2");

    userList.add(userNameMapObj1);
    userList.add(userNameMapObj2);

    userNameMap.put("users", userList);
    serviceAccountsResponse.put("service", userNameMap);
    ResponseEntity responseEntityServiceAccount =
        new ResponseEntity<>(serviceAccountsResponse, HttpStatus.OK);

    when(restTemplate.exchange(
            eq(getServiceAccountUri),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Object>) any()))
        .thenReturn(responseEntityServiceAccount);

    Set<String> response = aivenApiService.getServiceAccountUsers("testproject", "testservice");
    assertThat(response).contains("user1", "user2");
  }

  // Get service accounts
  @Test
  public void getServiceAccountsDontExist() {
    // get service account stubs
    String getServiceAccountUri = ACLS_BASE_URL + "testproject" + "/service/" + "testservice";

    Map<String, Map<String, Object>> serviceAccountsResponse = new HashMap<>();
    ResponseEntity responseEntityServiceAccount =
        new ResponseEntity<>(serviceAccountsResponse, HttpStatus.OK);

    when(restTemplate.exchange(
            eq(getServiceAccountUri),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Object>) any()))
        .thenReturn(responseEntityServiceAccount);

    Set<String> response = aivenApiService.getServiceAccountUsers("testproject", "testservice");
    assertThat(response).hasSize(0);
  }

  @Test
  public void listAcls() throws Exception {
    String getAclsUrl = ACLS_BASE_URL + "testproject" + "/service/" + "testservice" + "/acl";

    Map<String, List<Map<String, String>>> aclsResp = getAclListMap("testuser");
    ResponseEntity<Map<String, List<Map<String, String>>>> responseEntityServiceAccount =
        new ResponseEntity<>(aclsResp, HttpStatus.OK);

    when(restTemplate.exchange(
            eq(getAclsUrl),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Map<String, List<Map<String, String>>>>) any()))
        .thenReturn(responseEntityServiceAccount);

    Set<Map<String, String>> acls = aivenApiService.listAcls("testproject", "testservice");
    assertThat(acls).hasSize(6);
  }

  private static Map<String, List<Map<String, String>>> getAclListMap(String userName) {
    Map<String, String> aclMap1 = Map.of("permission", "read");
    Map<String, String> aclMap2 = Map.of("permission", "write");
    Map<String, String> aclMap3 = Map.of("permission", "ADMIN");
    Map<String, String> aclMap4 = Map.of("permission", "READWRITE");
    Map<String, String> aclMap5 = Map.of("id", "ID");
    Map<String, String> aclMap6 = Map.of("topic", "TOPIC");
    Map<String, String> aclMap7 =
        Map.of("username", userName, "topic", "testtopic", "permission", "write");

    List<Map<String, String>> aclList =
        List.of(aclMap1, aclMap2, aclMap3, aclMap4, aclMap5, aclMap6, aclMap7);

    Map<String, List<Map<String, String>>> aclsResp = Map.of("acl", aclList);
    return aclsResp;
  }

  @Test
  public void listAclsFailure() throws Exception {
    String getAclsUrl = ACLS_BASE_URL + "testproject" + "/service/" + "testservice" + "/acl";

    when(restTemplate.exchange(
            eq(getAclsUrl),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Map<String, List<Map<String, String>>>>) any()))
        .thenThrow(new RestClientException(""));

    AbstractThrowableAssert<?, ? extends Throwable> exception =
        assertThatThrownBy(() -> aivenApiService.listAcls("testproject", "testservice"));
    exception.isInstanceOf(Exception.class);
    exception.hasMessage("Error in listing acls : ");
  }

  @Test
  public void deleteAclsTestAndServiceUser() throws Exception {
    String projectName = "testproject";
    String serviceName = "testservice";

    ClusterAclRequest clusterAclRequest =
        ClusterAclRequest.builder()
            .aivenAclKey("4322342")
            .projectName(projectName)
            .serviceName(serviceName)
            .username(TESTUSER)
            .build();

    handleListAcls(projectName, serviceName, "testuser1"); // different user association

    String actual = aivenApiService.deleteAcls(clusterAclRequest).getMessage();
    String expected = ApiResultStatus.SUCCESS.value;

    assertThat(actual).isEqualTo(expected);
    String uri =
        SERVICE_ACLS_ENDPOINT
            .replace(PROJECT_NAME, projectName)
            .replace(SERVICE_NAME, serviceName)
            .replace("userName", TESTUSER);

    verify(restTemplate, times(1))
        .exchange(eq(uri), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Object.class));
  }

  @Test
  public void deleteAclsTestAndNotServiceUser() throws Exception {
    String projectName = "testproject";
    String serviceName = "testservice";

    ClusterAclRequest clusterAclRequest =
        ClusterAclRequest.builder()
            .aivenAclKey("4322342")
            .projectName(projectName)
            .serviceName(serviceName)
            .username(TESTUSER)
            .build();

    handleListAcls(projectName, serviceName, TESTUSER); // same user association

    String actual = aivenApiService.deleteAcls(clusterAclRequest).getMessage();
    String expected = ApiResultStatus.SUCCESS.value;

    assertThat(actual).isEqualTo(expected);
    String uri =
        SERVICE_ACLS_ENDPOINT
            .replace(PROJECT_NAME, projectName)
            .replace(SERVICE_NAME, serviceName)
            .replace("userName", TESTUSER);

    verify(restTemplate, times(0)) // service user is not deleted
        .exchange(eq(uri), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Object.class));
  }

  private void handleListAcls(String projectName, String serviceName, String userName) {
    Map<String, List<Map<String, String>>> aclsResp = getAclListMap(userName);
    ResponseEntity<Map<String, List<Map<String, String>>>> responseEntityServiceAccount =
        new ResponseEntity<>(aclsResp, HttpStatus.OK);

    String getAclsUrl = ACLS_BASE_URL + projectName + "/service/" + serviceName + "/acl";
    when(restTemplate.exchange(
            eq(getAclsUrl),
            eq(HttpMethod.GET),
            any(),
            (ParameterizedTypeReference<Map<String, List<Map<String, String>>>>) any()))
        .thenReturn(responseEntityServiceAccount);
  }

  @Test
  public void deleteAclsTestFailure() throws Exception {
    ClusterAclRequest clusterAclRequest =
        ClusterAclRequest.builder()
            .aivenAclKey("4322342")
            .projectName("testproject")
            .serviceName("serviceName")
            .build();
    String aclsUrl =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/acl/"
            + clusterAclRequest.getAivenAclKey();
    when(restTemplate.exchange(
            eq(aclsUrl), eq(HttpMethod.DELETE), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new RuntimeException(""));

    AbstractThrowableAssert<?, ? extends Throwable> exception =
        assertThatThrownBy(() -> aivenApiService.deleteAcls(clusterAclRequest));
    exception.isInstanceOf(Exception.class);
    exception.hasMessage("Error in deleting acls ");
  }

  @Test
  public void deleteAclsTestFailure404() throws Exception {
    ClusterAclRequest clusterAclRequest =
        ClusterAclRequest.builder()
            .aivenAclKey("4322342")
            .projectName("testproject")
            .serviceName("serviceName")
            .build();
    String aclsUrl =
        ACLS_BASE_URL
            + clusterAclRequest.getProjectName()
            + "/service/"
            + clusterAclRequest.getServiceName()
            + "/acl/"
            + clusterAclRequest.getAivenAclKey();
    when(restTemplate.exchange(
            eq(aclsUrl), eq(HttpMethod.DELETE), any(HttpEntity.class), any(Class.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    String actual = aivenApiService.deleteAcls(clusterAclRequest).getMessage();
    String expected = ApiResultStatus.SUCCESS.value;

    assertThat(actual).isEqualTo(expected);
  }
}
