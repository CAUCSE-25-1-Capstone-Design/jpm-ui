import jpm_core.jpm_core_function
from openai import OpenAI
import os
import tools
import json
from jpm_core import jpm_core_function
import sys
from utils import print_debug, save_gpt, save_user, read_all, read_last_n_chars, print_ui, print_progress
import io
import yaml
with open('config.yaml') as f:
    conf = yaml.safe_load(f)

with open('key.yaml') as f:
    conf_key = yaml.safe_load(f)
 
GPT_KEY = conf_key['gpt-key']
GPT_VERSION=conf['gpt-version']

if sys.platform.startswith('win'):
    sys.stdout = io.TextIOWrapper (sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper (sys.stderr.buffer, encoding='utf-8')

metaData=jpm_core_function.no_args_jpm("getMeta")

myTools=tools.tools
def query_process(query):
    save_user(query)
    chat_log=read_last_n_chars() # 이전의 대화 로그를 가져옴
    
    
#나중에 환경변수로 처리하기
    os.environ['OPENAI_API_KEY'] = GPT_KEY
    client = OpenAI()

    tools=myTools
    
    input_messages=[{"role": "system", 
                                "content": f'''
                            너는 사용자의 자연어 명령을 받아 해당 명령을 수행하기 위해 알맞은 Java 함수(tool)를 호출하는 시스템이다.

                            다음 규칙을 반드시 따르도록 한다:

                            1. 사용자가 패키지를 **설치하라고 요청**할 경우:
                            - 먼저 **Maven Central**에서 검색하여 정확한 `패키지 이름(name)`과 `배포 기관명(organization)`을 확인한다.
                            - 이를 기반으로 `install` 함수를 호출한다.

                            2. 사용자가 패키지를 **삭제하거나 업데이트하라고 요청**할 경우:
                            - 현재 설치된 패키지 목록을 확인하기 위해 반드시 먼저 하단의 메타데이터를 확인한다.
                            - 그 결과에서 일치하는 패키지를 찾아 해당 `name`과 `organization` 정보를 이용해 `delete` 또는 `update` 함수를 호출한다.
                            - 삭제 또는 업데이트할 패키지를 배열로 하여 함수의 인자로 건네준다.
                            - 목록에 존재하지 않으면 함수 호출을 시도하지 않는다.

                            3. `list`, `build`, `init`, `test`, `run` 등의 함수는 사용자의 명령이 명확히 해당 기능일 경우에만 호출한다.

                            추가적인 조건:
                            - 패키지 이름이 모호하거나 불완전한 경우, 유사한 이름을 Maven Central에서 찾아 보정한다.
                            - 함수 호출 결과를 받은 후에는 반드시 해당 결과를 반영해 다음 행동을 결정해야 한다.
                            - 이전의 대화 기록이 같이 제공되므로, 이를 활용한 대답을 해도 된다.
                            - 제공되는 메타데이터 역시 참고하여 대답한다.
                            - 대답 시에는 마크다운 문법을 제거하고 plain text로 대답한다.

                            예시:
                            - "junit 설치해줘" → Maven에서 `junit`에 대한 정확한 정보 검색 → install(name, organization)
                            - "log4j 삭제해줘" → 하단의 메타데이터 확인 → 설치 목록에서 log4j 찾기 → delete(name, organization)
                            - "프로젝트 실행해줘" → run()

                            개발과 관련 없는 이외의 질문은 무시해.

                            이전 대화 내역은 다음과 같음:
                            
                            <이전 대화내역 시작>
                            {chat_log}
                            <이전 대화내역 끝>

                            java 프로젝트의 메타 데이터는 다음과 같음:
                            <메타데이터 시작>
                            {metaData}
                            <메타데이터 끝>
                            '''},
                    {"role": "user", 
                    "content": query}
                    ]
        
        ## 기관명이랑 버전도 가져와야 함.

    print_progress("GPT","generate", "")
    response = client.responses.create(
        model=GPT_VERSION,
        input=input_messages,
        tools=tools,
        tool_choice="auto",
        temperature=0
    )

    #첫 결과(전체)
    print_debug(response.output)

    response_list=[]

    #type: function_call, message

    for tool_call in response.output:
        tool_call_process(tool_call, input_messages, client)
        
    return


    # for tool_call in response.output:
    #     #각각의 tool_call 에 대하여 query chaining이 끝날 때까지 gpt를 호출해야 함.
    #     # chaining 시에는 append로 모든 답안을 합해서 보낼 것. 누적으로.
    #     #각 tool_call의 최종 답은 response_list에 담아서 보낼 것.
    #     #print(tool_call)
    #     args=json.loads(tool_call.arguments)
    #     #print(args)
        


    #     result=jpm_core_function.jpm_caller(tool_call)

        
    #     #print(result)

    #     input_messages.append(tool_call)
    #     input_messages.append({
    #         "type": "function_call_output",
    #         "call_id": tool_call.call_id,
    #         "output": str(result)
    #     })


    #     response_2 = client.responses.create(
    #     model="gpt-4.1",
    #     input=input_messages,
    #     tools=tools,
    #         )
        
    #     print("아웃풋:"+ str(response_2.output))
    #     response_list.append(response_2.output_text)

    # return response_list[len(response_list)-1]

# 재귀적으로 tool_call 처리하는 함수
def tool_call_process(tool_call, input_messages, client):

#이 부분 최적화 필요. text찾기
# 메시지일 때만 ui에 출력하도록 함.
    if(tool_call.type=="message"):
        for response in tool_call.content:
            print_ui(response.text)
            save_gpt(response.text)
        return
    
    

    result = jpm_core_function.jpm_caller(tool_call)
   

    input_messages.append(tool_call)
    input_messages.append({
        "type": "function_call_output",
        "call_id": tool_call.call_id,
        "output": str(result)
    })
    
    response_2 = client.responses.create(
        model=GPT_VERSION,
        input=input_messages,
        tools=myTools,
        )

    #for tool_call in response_2.output: 
    tool_call_process(response_2.output[0], input_messages, client)

    


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print_ui("입력을 제공해주세요.")
        sys.exit(1)

    query = sys.argv[1]
    result = query_process(query)
    #print(result)