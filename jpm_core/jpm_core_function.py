
import subprocess
import json
from utils import print_debug, show_progress, print_progress
import yaml

with open('config.yaml') as f:
    conf = yaml.safe_load(f)
jpm_core_loc=conf['jpm-core-loc']

#tool_call을 받아서 파싱한 후 jpm 호출
def jpm_caller(tool_call):
    fn_name=tool_call.name
    #arguments='{"packages":[{"name":"bluecove","organization":"net.sf.bluecove"},{"name":"mysql-connector-java","organization":"mysql"}]}'
    args = json.loads(tool_call.arguments)
    print_debug("jpm-core call: " + fn_name)

    #  함수명에 따라 jpm-core 호출해야 함.
    match fn_name:
        case "install"|"delete"|"update":

            ## to be implemented
            # organization:name으로 붙여.
            # 패키지 리스트로 분해해야 함
            org_and_name_list=[]
            jpm_core_result=""
            for package in args['packages']:
                org_and_name=[package['organization']+":"+package['name']]
                print_progress("JPM", fn_name, org_and_name[0])
                result=one_args_jpm(fn_name, org_and_name)
                print_debug("\n-------------Core 출력 결과-------------\n" + result + "\n-------------출력완료-------------\n")
                jpm_core_result+=result
           
            print_debug(org_and_name_list)


            no_args_jpm("refresh")
            print_debug("refresh 완료")
            return jpm_core_result
        
        


        case "list"|"build"|"init"|"test"|"run":

            return no_args_jpm(fn_name)
        

        ## 메인 클래스 설정
        case "set":

            arg = ["--main="+ args["name"]]


            return one_args_jpm(fn_name, arg)
        
       
        

    return "잘못된 행동입니다.."    


# 인자가 없는(init, build 등) jpm 호출 프로세스

def no_args_jpm(fn_name):
    result = subprocess.run(
                # 여기에 인자 없이 어떻게 넣는거임? 그냥 비워두면 되나 체크하기
                ['java', '-jar', jpm_core_loc, fn_name,],
                capture_output=True,
                text=True
            )

            # print(result.stdout)

    # print("STDERR:")
    # print(result.stderr)
            # 추후에 예외처리 필요.


    return "STDOUT:"+ result.stdout+";\n STDERR:"+result.stderr+";"


def one_args_jpm(fn_name, arg):

    result = subprocess.run(
        ['java', '-jar', jpm_core_loc, fn_name]+arg,
        capture_output=True,
        text=True
    )

    # print(result.stdout)

    # print("STDERR:")
    #print(result.stderr)
    # 추후에 예외처리 필요.

    return "STDOUT:"+ result.stdout+"; STDERR:"+result.stderr+";"