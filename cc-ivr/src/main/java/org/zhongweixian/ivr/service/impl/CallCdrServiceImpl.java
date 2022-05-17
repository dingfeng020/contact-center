package org.zhongweixian.ivr.service.impl;

import org.cti.cc.entity.CallDetail;
import org.cti.cc.entity.CallDevice;
import org.cti.cc.entity.CallLog;
import org.cti.cc.entity.PushLog;
import org.cti.cc.mapper.CallDetailMapper;
import org.cti.cc.mapper.CallDeviceMapper;
import org.cti.cc.mapper.CallLogMapper;
import org.cti.cc.mapper.PushLogMapper;
import org.cti.cc.mapper.base.BaseMapper;
import org.cti.cc.po.CallLogPo;
import org.cti.cc.util.DateTimeUtil;
import org.cti.cc.util.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.zhongweixian.ivr.service.CallCdrService;

import java.util.List;

/**
 * Create by caoliang on 2020/10/28
 */
@Component
public class CallCdrServiceImpl extends BaseServiceImpl<CallLog> implements CallCdrService {

    @Autowired
    private CallLogMapper callLogMapper;

    @Autowired
    private CallDetailMapper callDetailMapper;

    @Autowired
    private CallDeviceMapper callDeviceMapper;

    @Autowired
    private PushLogMapper pushLogMapper;


    @Value("${call.cdr.mq:0}")
    private Integer callCdrMq;


    @Override
    BaseMapper<CallLog> baseMapper() {
        return callLogMapper;
    }

    @Override
    public int saveCallDevice(CallDevice callDevice) {
        if (callCdrMq == 1) {
            //rabbitTemplate.convertAndSend(Constants.CALL_DEVICE_EXCHANGE, Constants.CALL_CDR_ROUTING, JSON.toJSONString(callDevice));
            return 0;
        }
        return callDeviceMapper.insertSelective(callDevice);
    }

    @Override
    public int saveCallDetail(List<CallDetail> callDetails) {
        if (CollectionUtils.isEmpty(callDetails)) {
            return 0;
        }
        callDetails.forEach(callDetail -> {
            // rabbitTemplate.convertAndSend(Constants.CALL_DETAIL_EXCHANGE, Constants.CALL_CDR_ROUTING, JSON.toJSONString(callDetail));
        });
        return 1;
    }

    @Override
    public int saveOrUpdateCallLog(CallLog callLog) {
        if (callLog == null) {
            return 0;
        }
        logger.info("callId:{} , answerTime:{}", callLog.getCallId(), callLog.getAnswerTime());
        if (callCdrMq == 1) {
            // rabbitTemplate.convertAndSend(Constants.CALL_LOG_EXCHANGE, Constants.CALL_CDR_ROUTING, JSON.toJSONString(callLog));
            return 0;
        }

        if (callLog.getAnswerTime() != null && callLog.getEndTime() == null) {
            //呼通
            return callLogMapper.insertSelective(callLog);
        }
        if (callLog.getAnswerTime() == null && callLog.getEndTime() != null) {
            //没有呼通
            return callLogMapper.insertSelective(callLog);
        }
        int result = callLogMapper.updateByCallId(callLog);
        if (result == 0) {
            result = callLogMapper.insertSelective(callLog);
        }
        return result;
    }

    @Override
    public CallLogPo getCall(Long companyId, Long callId) {
        /**
         * 解析callId产生的时间
         */
        String binaryString = Long.toBinaryString(callId);
        Long time = Long.parseLong(binaryString.substring(0, 36), 2) + SnowflakeIdWorker.WORK_START;
        logger.info("callId:{} callTime:{} ", callId, DateTimeUtil.format(time));
        return callLogMapper.getCall(companyId, callId, DateTimeUtil.getMonth(time));
    }

    @Override
    public int savePushFailLog(PushLog pushFailLog) {
        return pushLogMapper.insertSelective(pushFailLog);
    }
}
