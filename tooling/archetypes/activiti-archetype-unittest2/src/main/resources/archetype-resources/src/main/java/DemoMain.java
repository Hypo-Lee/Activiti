package ${package};

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 　　* @Description:
 * 　　* @ClassName DemoMain
 * 　　* @Author Hypo
 * 　　* @Date 2019/12/30 14:03
 *
 */
public class DemoMain {

    public static void main(String[] args) throws ParseException {
        Logger logger = LoggerFactory.getLogger(DemoMain.class);

        //创建引擎
        ProcessEngine processEngine = createProcessEngine(logger);
        //部署流程
        ProcessDefinition processDefinition = getProcessDefinition(logger, processEngine);
        //获取流程实例
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

        //获取处理任务
        dealTask(logger, processEngine, runtimeService, processInstance);
    }

    private static void dealTask(Logger logger, ProcessEngine processEngine, RuntimeService runtimeService, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        TaskService taskService = processEngine.getTaskService();
        while(processInstance != null && processInstance.getId() != null){
            List<Task> list = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            FormService formService = processEngine.getFormService();
            for (Task task : list) {
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                logger.info(task.getName());
                Map<String,Object> paramVariable = Maps.newHashMap();
                for (FormProperty formProperty : taskFormData.getFormProperties()) {
                    if(StringFormType.class.isInstance(formProperty.getType())){
                        logger.info("请填写"+formProperty.getName());
                        paramVariable.put(formProperty.getId(),scanner.nextLine());
                    }else if(DateFormType.class.isInstance(formProperty.getType())){
                        logger.info("请填写"+formProperty.getName());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        paramVariable.put(formProperty.getId(),sdf.parse(scanner.next()));
                    }else{
                        logger.info("暂不支持此类型");
                    }
                }
                taskService.complete(task.getId(),paramVariable);
                processInstance = runtimeService.createProcessInstanceQuery().singleResult();
            }
        }
        logger.info("流程结束");
        scanner.close();
    }

    private static ProcessDefinition getProcessDefinition(Logger logger, ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("secondary_approval.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();
        String deploymentId = deploy.getId();
        logger.info("流程部署id：{}",deploymentId);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        String name = deploy.getName();
        logger.info("流程定义id：{}   流程文件名称：{}",processDefinition.getId(),processDefinition.getName());
        return processDefinition;
    }

    private static ProcessEngine createProcessEngine(Logger logger) {
        ProcessEngine processEngine = ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("activiti.cfg.xml")
                .buildProcessEngine();
        logger.info("流程引擎版本：{}   流程引擎名称：{}",ProcessEngine.VERSION,processEngine.getName());
        return processEngine;
    }

}
