"""Run the packaged Java resolver/invoker, including PowerShell command shadowing."""
import os
import pathlib
import shutil
import subprocess
import tempfile
import unittest

R = pathlib.Path(__file__).resolve().parents[1]
PS = pathlib.Path(os.environ.get('POWERSHELL_EXECUTABLE', shutil.which('pwsh') or 'pwsh'))
JAVA = pathlib.Path(os.environ.get('JAVA8', shutil.which('java') or 'java')).resolve()
PACKAGE = pathlib.Path(os.environ.get('PACKAGE_DIR', str(R if (R/'package.json').exists() else R/'dist/VelvetG-Lean-Overhaul2')))
HARNESS = r'''
$ErrorActionPreference='Stop'
Set-StrictMode -Version 2
$tokens=$null;$parseErrors=$null
$tree=[Management.Automation.Language.Parser]::ParseFile($env:VELVET_INSTALLER_SOURCE,[ref]$tokens,[ref]$parseErrors)
if($parseErrors.Count -ne 0){throw 'Installer parse failed'}
foreach($wanted in @('Resolve-VelvetJava','Invoke-VelvetJava')){
 $definition=$tree.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $wanted},$true)
 if($null -eq $definition){throw 'Packaged Java helper missing'}
 . ([ScriptBlock]::Create($definition.Extent.Text))
}
# Reproduce the old case-insensitive function collision and also exclude aliases.
function Java {throw 'Function shadow was executed'}
function java.exe {throw 'Function shadow was executed'}
$script:javaExecutable=Resolve-VelvetJava $env:VELVET_REQUESTED_JAVA
if($script:javaExecutable -ne $env:VELVET_EXPECTED_JAVA){throw ('Wrong executable selected: '+$script:javaExecutable)}
Invoke-VelvetJava -arguments @('-version')
'''

class JavaDiscovery(unittest.TestCase):
 def test_discovery_and_execution(self):
  with tempfile.TemporaryDirectory(prefix='VelvetG Java ! & ') as td:
   root=pathlib.Path(td)
   home=JAVA.parent.parent
   if os.name!='nt':
    # Resolve and really execute Java through a home path containing shell metacharacters.
    spaced_home=root/'Java home ! &'
    spaced_home.symlink_to(home,target_is_directory=True)
   else:
    spaced_home=home
   cases=[
    ('PATH with shadow functions',None,None,str(JAVA),True),
    ('JAVA_HOME',str(home),None,str(home/'bin'/JAVA.name),True),
    ('quoted JAVA_HOME with spaces',' "'+str(spaced_home)+'" ',None,str(spaced_home/'bin'/JAVA.name),True),
    ('stale JAVA_HOME fallback',str(root/'missing home'),None,str(JAVA),True),
    ('explicit quoted path',str(root/'missing home'),' "'+str(spaced_home/'bin'/JAVA.name)+'" ',str(spaced_home/'bin'/JAVA.name),True),
    ('explicit path overrides JAVA_HOME',str(home),str(JAVA),str(JAVA),True),
    ('invalid explicit path refuses fallback',str(home),str(root/'missing java.exe'),str(JAVA),False),
    ('missing Java is actionable',None,None,'',False),
   ]
   for name,home_value,requested,expected,ok in cases:
    with self.subTest(name=name):
     env=dict(os.environ)
     env.update(XDG_CACHE_HOME=str(root/'cache'),XDG_CONFIG_HOME=str(root/'config'),XDG_DATA_HOME=str(root/'data'),VELVET_INSTALLER_SOURCE=str(PACKAGE/'Manage-VelvetG.ps1'),VELVET_REQUESTED_JAVA=requested or '',VELVET_EXPECTED_JAVA=expected)
     env['PATH']=str(JAVA.parent) if expected else ''
     env.pop('JAVA_HOME',None)
     if home_value is not None:env['JAVA_HOME']=home_value
     result=subprocess.run([str(PS),'-NoProfile','-Command',HARNESS],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
     if ok:
      self.assertEqual(result.returncode,0,result.stdout)
      self.assertIn('1.8.0',result.stdout)
     else:
      self.assertNotEqual(result.returncode,0,result.stdout)
      self.assertIn('Java executable not found' if requested else 'Java 8 or newer was not found',result.stdout)
   print('Java discovery: all 8 scenarios passed, including command shadowing and actual Java 8 execution.')

if __name__=='__main__':unittest.main()
