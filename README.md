# 체인코드 테스트하기

## 테스트 환경 준비
MacOS에 아래 소프트웨어를 설치한다.
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [Nodejs](https://nodejs.org/en/download/) 또는 Java

이 repo를 clone한다.
```
git clone https://oss.navercorp.com/ncloud-paas/blockchain-testing.git
```

## 체인코드 설치 및 테스트

### 1. 체인코드 파일 준비

docker desktop을 실행하고, 실행이 완료될때까지 기다린다.  
터미널에서 build_cds.sh 스크립트를 실행해서 체인코드 파일들을 생성한다.  
cds_files 라는 폴더에 다음과 같은 파일들이 생성된다.

| 파일명 | 내용 |
|---|---|
| fabcar_go.v1.cds | 체인코드명: fabcar_go 버전: 1.0 언어: go |
| fabcar_go.v2.cds | 체인코드명: fabcar_go 버전: 2.0 언어: go |
| fabcar_java.v1.cds | 체인코드명: fabcar_java 버전: 1.0 언어: java |
| fabcar_java.v2.cds | 체인코드명: fabcar_java 버전: 2.0 언어: java |
| fabcar_javascript.v1.cds | 체인코드명: fabcar_javascript 버전: 1.0 언어: javascript |
| fabcar_javascript.v2.cds | 체인코드명: fabcar_javascript 버전: 2.0 언어: javascript |

### 2. 체인코드 설치 및 인스턴스화

블록체인 서비스에서 피어 노드를 생성하고, 이 피어 노드에 앞에서 준비한 체인코드들을 설치한다.  
`defaultchannel`이란 이름으로 채널을 생성하고, 이 채널에 피어를 가입시킨다.

설치한 체인코드를 `defaultchannel`에 인스턴스화 시킨다. 3가지 언어의 체인코드가 모두 정상적으로 인스턴스화되는지 확인한다.
- 초기화 함수명: initLedger
- 인자: 없음

### 3. 커넥션 프로파일 및 아이텐티티 다운로드

블록체인 서비스의 조직 메뉴에서 커넥션 프로파일을 다운로드 받아 `client/javascript/download` 폴더에 `connection_profile.json`이란 이름으로 저장한다.  
CA의 아이텐티티 메뉴에서 client type의 사용자를 export해서 `client/javascript/download` 폴더에 `user.json`이란 이름으로 저장한다.

### 4. 체인코드 실행

터미널에서 `client/javascript` 폴더로 이동한 후 아래와 같이 체인코드를 실행한다. (체인코드 실행에선 fabcar_go 체인코드만 사용하므로
다른 언어의 체인코드는 설치하지 않아도 상관없음)
```
cd client/javascript
npm install
node query.js
node invoke.js
```

Java app을 이용할 경우엔 아래와 같이 실행한다.
```
cd client/java
mvn compile
mvn exec:java -Dexec.mainClass=org.example.ClientApp
```

## 컨소시엄 구성에 따른 체인코드 테스트

체인코드 테스트를 위한 가장 단순한 구성은 1개 조직, 1개 피어만 준비된 상태에서 위 테스트를 진행하는 것이다.  
좀더 다양한 테스트를 위해선 아래과 같은 구성을 고려할 수 있다.

| 구성 | 기대 결과 |
---|---
1개 조직에 여러 개 피어에 체인코드 설치 및 채널조인 | 성공
2개 조직에 하나 이상씩의 피어, 보증정책 2/2 | 성공
2개 조직중 한쪽의 피어에만 체인코드 설치, 보증정책 2/2 | 실패
2개 조직중 한쪽의 피어에만 체인코드 설치, 보증정책 1/2 | 성공
서로 다른 네트워크의 2개 조직에 하나 이상씩의 피어, 보증정책 2/2 | 성공

* 서로 다른 네트워크의 조직으로 테스트하는 경우, 해당 채널에 각 조직의 앵커피어가 설정되어 있어야 함

# 벤치마크

## 1. java를 이용한 벤치마크 테스트
java client를 활용해 간단한 벤치마크를 수행한다. 실행준비 및 방법은 체인코드 테스트와 동일하며, `fabcar_go` 체인코드를 이용한다.

`benchmark/agent/download/` 폴더에 connection_profile.json과 user.json을 다운로드하고 아래와 같이 벤치마크를 실행한다.

```
cd benchmark/agent
mvn compile
mvn exec:java -Dexec.mainClass=com.naver.agent.fabcar.Benchmark
```

아래와 같이 결과가 출력된다.
```
Start dummy Benchmark ...
dummy Benchmark finished in 143ms
Warming up...
Warming up Done
Start write tx ...
write tx finished.
20000 tx in 64742ms
TPS: 308
Sleeping some seconds...
Start read tx ...
read tx finished.
20000 tx in 28863ms
TPS: 692
```

## 2. caliper를 이용한 벤치마크 테스트
caliper를 이용해 벤치마크를 수행한다. [실행 준비과정](#체인코드-설치-및-테스트)은 동일하며, caliper 및 관련툴을 설치하는 과정이 추가로 필요하다.

### caliper 및 관련 툴 설치
아래 툴들을 설치한다.
- nodejs 10 (8.x나 12.x는 안됨)
- npm
- npx
- envsubst
- jq

다음과 같이 caliper를 설치한다.
```
cd `benchmark/caliper`
npm install
npx caliper bind --caliper-bind-sut fabric:1.4
```

### 벤치마크 실행하기
먼저 `benchmark/caliper/download/` 폴더에 connection_profile.json과 user.json을 다운로드하고 `prepare_test.sh`를 실행한다.
`run_test.sh`를 실행하면 다음과 같은 결과를 얻을수 있고, `report.html`이 생성된다.

```
+---------+-------+------+-----------------+-----------------+-----------------+-----------------+------------------+
| Name    | Succ  | Fail | Send Rate (TPS) | Max Latency (s) | Min Latency (s) | Avg Latency (s) | Throughput (TPS) |
|---------|-------|------|-----------------|-----------------|-----------------|-----------------|------------------|
| readTx  | 30707 | 0    | 511.8           | 0.11            | 0.00            | 0.01            | 511.7            |
|---------|-------|------|-----------------|-----------------|-----------------|-----------------|------------------|
| writeTx | 26800 | 0    | 446.6           | 20.30           | 0.08            | 13.17           | 381.4            |
+---------+-------+------+-----------------+-----------------+-----------------+-----------------+------------------+
```
