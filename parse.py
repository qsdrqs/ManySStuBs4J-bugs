#!/usr/bin/env python3
'''
Copyright (C) 2024 qsdrqs

Author: qsdrqs <qsdrqs@gmail.com>
All Right Reserved

This file is the parser of the bug dataset.

'''

import requests
import os
import subprocess
import json
import time
import tqdm
import sys

json_files = []


def get_file_from_github(repo, hash, path):
    url = f'https://raw.githubusercontent.com/{repo}/{hash}/{path}'
    return requests.get(url).text


def parse_json(file):
    file_without_ext = file.split('.')[0]
    os.makedirs(f'./bugs_collection/{file_without_ext}', exist_ok=True)
    with open(f'./files-archive/{file}') as f:
        datas = json.load(f)
        '''
        {
            "fixCommitSHA1": "e1b4341bcf2075abbb30b3336aa889a04f683cd4",
            "fixCommitParentSHA1": "e15dbdf6aceac531383d22369d7f09b40106f06e",
            "bugFilePath": "modules/activiti-secure-service-tasks/src/test/java/org/activiti/test/serviceTask/secure/SecureShellTaskTest.java",
            "fixPatch": "",
            "projectName": "Activiti.Activiti",
            "bugLineNum": 41,
            "bugNodeStartChar": 1519,
            "bugNodeLength": 19,
            "fixLineNum": 41,
            "fixNodeStartChar": 1519,
            "fixNodeLength": 18,
            "sourceBeforeFix": "\"shellCommandStart\"",
            "sourceAfterFix": "\"shellCommandEcho\""
        },
        '''
        index = 0
        print(f'Parsing {file}...')
        print(f'Found {len(datas)} bugs')
        for data in tqdm.tqdm(datas):
            if os.path.exists(f'./bugs_collection/{file_without_ext}/{index}/src.patch'):
                index += 1
                continue

            repo = data['projectName'].split('.')
            #print(repo)
            #assert len(repo) == 2
            repo = '/'.join(repo)

            path = data['bugFilePath']
            path_dir = path.split('/')
            path_dir.pop()
            path_dir = '/'.join(path_dir)

            buggy_hash = data['fixCommitParentSHA1']
            fixed_hash = data['fixCommitSHA1']

            buggy_file = get_file_from_github(repo, buggy_hash, path)
            fixed_file = get_file_from_github(repo, fixed_hash, path)

            os.makedirs(
                f'./bugs_collection/{file_without_ext}/{index}/buggy/{path_dir}', exist_ok=True)
            with open(f'./bugs_collection/{file_without_ext}/{index}/buggy/{path}', 'w') as f:
                f.write(buggy_file)
            os.makedirs(
                f'./bugs_collection/{file_without_ext}/{index}/fixed/{path_dir}', exist_ok=True)
            with open(f'./bugs_collection/{file_without_ext}/{index}/fixed/{path}', 'w') as f:
                f.write(fixed_file)
            # run git diff
            subprocess.run(['git', 'diff', '--no-index', f'buggy/{path}', f'fixed/{path}'],
                           cwd=f'./bugs_collection/{file_without_ext}/{index}',
                           stdout=open(f'./bugs_collection/{file_without_ext}/{index}/src.patch', 'w'),
                           stderr=subprocess.PIPE)
            index += 1
            # sleep 100 ms
            time.sleep(0.1)


def main():
    if len(sys.argv) != 2:
        print('Usage: python parse.py <json_file>')
        sys.exit(1)

    os.makedirs('bugs_collection', exist_ok=True)

    # get all json files under ./files-archive
    files = os.listdir('./files-archive')
    for file in files:
        if file.endswith('.json'):
            json_files.append(file)


    for file in json_files:
        if file == sys.argv[1]:
            parse_json(file)

if __name__ == '__main__':
    main()
