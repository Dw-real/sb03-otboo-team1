# 🥼 옷장을 부탁해 - 개인화 의상 및 아이템 추천 SaaS

> 날씨, 취향을 고려해 사용자가 보유한 의상 조합을 추천해주고, OOTD 피드, 팔로우 등의 소셜 기능을 갖춘 서비스  

## 🔗 Notion

👉 [Notion 페이지](https://www.notion.so/ohgiraffers/ONE-PIECE-207649136c1180c587fdcdecf0eccd0d)

---

## 👥 Team1 - ONE-PIECE

| 이름 | GitHub                                    | 개발 기능         | 개발 외 역할|
|------|-------------------------------------------|---------------|---------------|
| 한동우 | [Dw-real](https://github.com/Dw-real) | 프로필 & 날씨데이터 관리   | 팀장, Git 형상관리|
| 김민준 | [MinJoon Kim](https://github.com/adjoon1) | 팔로우 & DM & 알림 관리| PM, 회의록 관리 |
| 박진솔 | [JinsolPark](https://github.com/JinsolPark) | 의상 & 추천 관리 | AWS 관리|
| 이승진 | [noonsong](https://github.com/noonsong0208) | OOTD 피드 & 댓글 관리 | 노션 관리, 발표 자료(PPT, 영상)|
| 조재구 | [NINE](https://github.com/NINE-J)    | 사용자 & 인증 인가 관리     |  DB 관리 |

---

## 🛠 기술 스택

| 분류       | 기술 |
|------------|------|
| **Backend** | Spring Boot 3.5.3 |
| **Database** | PostgreSQL 17.5, H2 |
| **API 문서화** | Swagger UI |
| **협업 도구** | Discord, GitHub, Notion |
| **일정 관리** | GitHub Issues + Notion Timeline |

---

## 📌 주요 기능 요약

### 한동우

- 날씨 데이터 관리
    - 날씨 위치 정보 조회
    - 날씨 정보 조회
    - 특별한 날씨 변화(기온 상승, 강수 등) 발생 시 알림 생성
- 프로필 관리
    - 회원가입
    - 사용자 목록 조회
    - 프로필 조회
    - 비밀번호 변경
    - 프로필 업데이트

---

### 김민준

- 팔로우와 DM 
    - 팔로우 생성
    - 팔로우 요약 정보 조회
    - 팔로잉 목록 조회
    - 팔로워 목록 조회
    - 팔로우 취소
    - DM 목록 조회
    - DM 전송   
- 알림 관리
    - SSE 연결/ 이벤트 발행
    - 알림 목록 조회

---

### 박진솔

- 의상 관리 및 추천
  - 의상 등록
  - 의상 속성 정의 등록
  - 의상 목록 조회
  - 의상 속성 정의 수정
  - 의상 정보 수정
  - 의상 속성 정의 목록 조회
  - 의상 속성 정의 삭제
  - 구매 링크로 옷 정보 불러오기
  - 날씨에 따른 의상 추천 기능

---

### 이승진

- OOTD 피드
  - 피드 등록
  - 피드 댓글 등록
  - 피드 좋아요 등록
  - 피드 목록 조회
  - 피드 댓글 조회
  - 피드 수정
  - 피드 삭제
  - 피드 좋아요 취소

---

### 조재구

- 인증 인가 관리
  - 로그인
  - 비밀번호 초기화
  - CSRF 토큰 조회
  - 토큰 재발급
  - 소셜계정 연동 (Google, Kakao)
  - 자동 로그아웃
  - 권한 변경
  - 계정 잠금 상태 변경

---

## 📁 프로젝트 구조

- 유지보수성과 역할 명확성을 높이기 위한 도메인 기반 패키지
<img width="318" height="521" alt="image" src="https://github.com/user-attachments/assets/a3879205-e1fc-4afa-b1a3-2c94a98c6e0d" />
<img width="304" height="522" alt="image" src="https://github.com/user-attachments/assets/81217bbd-1b13-4cc4-9495-6a85f1160e94" />

---
## 배포 URL

https://otboo.site

---

## 회고록

[https://www.notion.so/ohgiraffers/23f649136c118059b381dc88c93b4746](https://www.notion.so/codeit/3-28e6fd228e8d80cbbfe3d81346c0e30c?p=28e6fd228e8d80ee92d2daca7c0be36f&pm=s)

---

## 시연 영상
https://www.notion.so/ohgiraffers/26a649136c1180c1ad15e5da407f2d09?source=copy_link#296649136c11807189a9f2bb1c57efd1
