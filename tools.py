tools = [
        {
  "type": "function",
  "name": "install",
  "description": "여러 개의 패키지를 프로젝트에 설치합니다.",
  "parameters": {
    "type": "object",
    "properties": {
      "packages": {
        "type": "array",
        "description": "설치할 패키지 목록",
        "items": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "description": "패키지 이름"
            },
            "organization": {
              "type": "string",
              "description": "패키지를 배포한 기관명"
            }
          },
          "required": ["name", "organization"]
        }
      }
    },
    "required": ["packages"],
    "additionalProperties": False
  }
}
,
{
  "type": "function",
  "name": "delete",
  "description": "여러 개의 패키지를 프로젝트에서 삭제합니다.",
  "parameters": {
    "type": "object",
    "properties": {
      "packages": {
        "type": "array",
        "description": "삭제할 패키지 목록",
        "items": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "description": "패키지 이름"
            },
            "organization": {
              "type": "string",
              "description": "패키지를 배포한 기관명"
            }
          },
          "required": ["name", "organization"]
        }
      }
    },
    "required": ["packages"],
    "additionalProperties": False
  }
}
,
   {
  "type": "function",
  "name": "update",
  "description": "여러 개의 패키지를 최신 버전으로 업데이트합니다.",
  "parameters": {
    "type": "object",
    "properties": {
      "packages": {
        "type": "array",
        "description": "업데이트할 패키지 목록",
        "items": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "description": "패키지 이름"
            },
            "organization": {
              "type": "string",
              "description": "패키지를 배포한 기관명"
            }
          },
          "required": ["name", "organization"]
        }
      }
    },
    "required": ["packages"],
    "additionalProperties": False
  }
}

   ,
    {
        "type": "function",
        "name": "list",
        "description": "현재 프로젝트에 설치된 모든 패키지를 조회합니다.",
        "parameters": {
            "type": "object",
            "properties": {},
            "required": [],
            "additionalProperties": False
        }
    },
    {
        "type": "function",
        "name": "build",
        "description": "프로젝트를 빌드합니다.",
        "parameters": {
            "type": "object",
            "properties": {},
            "required": [],
            "additionalProperties": False
        }
    },
    {
        "type": "function",
        "name": "init",
        "description": "프로젝트를 초기화합니다.",
        "parameters": {
            "type": "object",
            "properties": {},
            "required": [],
            "additionalProperties": False
        }
    },
    {
        "type": "function",
        "name": "test",
        "description": "프로젝트 테스트를 수행합니다.",
        "parameters": {
            "type": "object",
            "properties": {},
            "required": [],
            "additionalProperties": False
        }
    },
    {
        "type": "function",
        "name": "run",
        "description": "프로젝트를 실행합니다.",
        "parameters": {
            "type": "object",
            "properties": {},
            "required": [],
            "additionalProperties": False
        }
    },

    {
      "type": "function",
      "name": "set",
      "description": "실행할 프로젝트의 메인 클래스를 설정합니다.",
      "parameters": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string",
            "description": "진입점 클래스 패키지명"
          }
        },
        "required": ["name"],
        "additionalProperties": False
      }
    }

   
]
