import React, { useState, useEffect } from 'react'
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  message,
  Descriptions,
  Badge,
  Space,
  Button,
  Input,
  Tabs,
  Alert,
  Divider,
  Progress,
  Modal,
  Tree,
} from 'antd'
import {
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  SearchOutlined,
  DownloadOutlined,
  ApartmentOutlined,
  BugOutlined,
} from '@ant-design/icons'
import reportService from '../../services/reportService'

const { TabPane } = Tabs
const { TextArea } = Input

/**
 * QualityReport Component
 * Quality report and traceability visualization
 *
 * Task: T072 [P] [US3] Create frontend components for 质量报告页面
 * User Story 3: 测试结果可视化分析
 */
const QualityReport = ({ taskId }) => {
  const [loading, setLoading] = useState(false)
  const [report, setReport] = useState(null)
  const [comprehensiveReport, setComprehensiveReport] = useState(null)
  const [requirementId, setRequirementId] = useState('')
  const [traceabilityData, setTraceabilityData] = useState(null)
  const [qualityGates, setQualityGates] = useState(null)
  const [showHtmlModal, setShowHtmlModal] = useState(false)
  const [htmlReport, setHtmlReport] = useState('')

  useEffect(() => {
    if (taskId) {
      loadReport()
      loadComprehensiveReport()
      loadQualityGates()
    }
  }, [taskId])

  const loadReport = async () => {
    setLoading(true)
    try {
      const response = await reportService.getReportByTaskId(taskId)
      if (response.success) {
        setReport(response.data)
      } else {
        message.error('Failed to load quality report')
      }
    } catch (error) {
      message.error(error.message || 'Failed to load quality report')
    } finally {
      setLoading(false)
    }
  }

  const loadComprehensiveReport = async () => {
    try {
      const response = await reportService.getComprehensiveReport(taskId)
      if (response.success) {
        setComprehensiveReport(response.data)
      }
    } catch (error) {
      console.error('Failed to load comprehensive report:', error)
    }
  }

  const loadQualityGates = async () => {
    try {
      const response = await reportService.checkQualityGates(taskId)
      if (response.success) {
        setQualityGates(response.data)
      }
    } catch (error) {
      console.error('Failed to load quality gates:', error)
    }
  }

  const handleTraceRequirement = async () => {
    if (!requirementId.trim()) {
      message.warning('Please enter a requirement ID')
      return
    }

    setLoading(true)
    try {
      const response = await reportService.traceRequirementCoverage(requirementId)
      if (response.success) {
        setTraceabilityData(response.data)
        message.success('Traceability data loaded successfully')
      }
    } catch (error) {
      message.error(error.message || 'Failed to load traceability data')
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadHtmlReport = async () => {
    try {
      const response = await reportService.getHtmlReport(taskId)
      setHtmlReport(response)
      setShowHtmlModal(true)
    } catch (error) {
      message.error('Failed to download HTML report')
    }
  }

  const getRiskLevelColor = (level) => {
    const colors = {
      LOW: 'success',
      MEDIUM: 'warning',
      HIGH: 'error',
      CRITICAL: 'error',
    }
    return colors[level] || 'default'
  }

  const getRiskLevelIcon = (level) => {
    const icons = {
      LOW: <CheckCircleOutlined style={{ color: '#52c41a' }} />,
      MEDIUM: <WarningOutlined style={{ color: '#faad14' }} />,
      HIGH: <CloseCircleOutlined style={{ color: '#ff4d4f' }} />,
      CRITICAL: <CloseCircleOutlined style={{ color: '#cf1322' }} />,
    }
    return icons[level] || <WarningOutlined />
  }

  const testResultColumns = [
    {
      title: '用例名称 (Test Case)',
      dataIndex: 'testCaseName',
      key: 'testCaseName',
      width: 300,
    },
    {
      title: '状态 (Status)',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => {
        const colors = {
          PASSED: 'success',
          FAILED: 'error',
          SKIPPED: 'default',
          BLOCKED: 'warning',
        }
        return <Tag color={colors[status] || 'default'}>{status}</Tag>
      },
    },
    {
      title: '执行时间 (Execution Time)',
      dataIndex: 'executionTime',
      key: 'executionTime',
      width: 150,
      render: (time) => `${time || 0}ms`,
    },
    {
      title: '执行时间 (Executed At)',
      dataIndex: 'executedAt',
      key: 'executedAt',
      width: 180,
      render: (date) => date ? new Date(date).toLocaleString('zh-CN') : 'N/A',
    },
    {
      title: '错误信息 (Error)',
      dataIndex: 'error',
      key: 'error',
      ellipsis: true,
      render: (error) => error || '-',
    },
  ]

  if (!report) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Card>
          <FileTextOutlined style={{ fontSize: '48px', color: '#d9d9d9' }} />
          <p style={{ marginTop: '16px', color: '#999' }}>
            {taskId ? 'Loading quality report...' : 'No task selected'}
          </p>
        </Card>
      </div>
    )
  }

  return (
    <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
      <Card
        title={
          <Space>
            <FileTextOutlined />
            <span>质量报告 (Quality Report)</span>
          </Space>
        }
        extra={
          <Space>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleDownloadHtmlReport}
            >
              导出HTML (Export HTML)
            </Button>
          </Space>
        }
        bordered={false}
      >
        {/* Summary Statistics */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="总用例数 (Total Cases)"
                value={report.testResults?.length || 0}
                prefix={<FileTextOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="通过 (Passed)"
                value={
                  report.testResults?.filter((r) => r.status === 'PASSED').length || 0
                }
                valueStyle={{ color: '#52c41a' }}
                prefix={<CheckCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="失败 (Failed)"
                value={
                  report.testResults?.filter((r) => r.status === 'FAILED').length || 0
                }
                valueStyle={{ color: '#ff4d4f' }}
                prefix={<CloseCircleOutlined />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="跳过 (Skipped)"
                value={
                  report.testResults?.filter((r) => r.status === 'SKIPPED').length || 0
                }
                valueStyle={{ color: '#d9d9d9' }}
              />
            </Card>
          </Col>
        </Row>

        {/* Risk Assessment */}
        {report.riskAssessment && (
          <Card
            title="风险评估 (Risk Assessment)"
            style={{ marginBottom: 24 }}
          >
            <Row gutter={16}>
              <Col span={24}>
                <Alert
                  message={
                    <Space>
                      {getRiskLevelIcon(report.riskAssessment.overallRisk)}
                      <span>
                        整体风险等级 (Overall Risk): {report.riskAssessment.overallRisk}
                      </span>
                    </Space>
                  }
                  description={
                    <div>
                      <p>
                        <strong>风险分数 (Risk Score):</strong>{' '}
                        {(report.riskAssessment.riskScore * 100).toFixed(1)}%
                      </p>
                      <Progress
                        percent={parseFloat((report.riskAssessment.riskScore * 100).toFixed(1))}
                        status={
                          report.riskAssessment.overallRisk === 'CRITICAL' ||
                          report.riskAssessment.overallRisk === 'HIGH'
                            ? 'exception'
                            : 'normal'
                        }
                      />
                    </div>
                  }
                  type={
                    report.riskAssessment.overallRisk === 'CRITICAL' ||
                    report.riskAssessment.overallRisk === 'HIGH'
                      ? 'error'
                      : report.riskAssessment.overallRisk === 'MEDIUM'
                      ? 'warning'
                      : 'success'
                  }
                  showIcon
                  style={{ marginBottom: 16 }}
                />
              </Col>
            </Row>

            {/* High Risk Modules */}
            {report.riskAssessment.highRiskModules &&
              report.riskAssessment.highRiskModules.length > 0 && (
                <div style={{ marginBottom: 16 }}>
                  <Divider orientation="left">高风险模块 (High Risk Modules)</Divider>
                  <Space wrap>
                    {report.riskAssessment.highRiskModules.map((module) => (
                      <Tag key={module} color="red" icon={<BugOutlined />}>
                        {module}
                      </Tag>
                    ))}
                  </Space>
                </div>
              )}

            {/* Recommendations */}
            {report.riskAssessment.recommendations &&
              report.riskAssessment.recommendations.length > 0 && (
                <div>
                  <Divider orientation="left">建议 (Recommendations)</Divider>
                  <ul>
                    {report.riskAssessment.recommendations.map((rec, index) => (
                      <li key={index}>{rec}</li>
                    ))}
                  </ul>
                </div>
              )}
          </Card>
        )}

        {/* Defect Statistics */}
        {report.defectStats && (
          <Card title="缺陷统计 (Defect Statistics)" style={{ marginBottom: 24 }}>
            <Row gutter={16}>
              <Col xs={24} sm={6}>
                <Statistic
                  title="严重 (Critical)"
                  value={report.defectStats.critical || 0}
                  valueStyle={{ color: '#cf1322' }}
                />
              </Col>
              <Col xs={24} sm={6}>
                <Statistic
                  title="重要 (Major)"
                  value={report.defectStats.major || 0}
                  valueStyle={{ color: '#ff4d4f' }}
                />
              </Col>
              <Col xs={24} sm={6}>
                <Statistic
                  title="次要 (Minor)"
                  value={report.defectStats.minor || 0}
                  valueStyle={{ color: '#faad14' }}
                />
              </Col>
              <Col xs={24} sm={6}>
                <Statistic
                  title="总计 (Total)"
                  value={report.defectStats.total || 0}
                />
              </Col>
            </Row>
          </Card>
        )}

        {/* Tabs for detailed information */}
        <Tabs defaultActiveKey="results">
          <TabPane tab="测试结果 (Test Results)" key="results">
            <Table
              columns={testResultColumns}
              dataSource={report.testResults || []}
              rowKey="testCaseId"
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `总计 ${total} 条 (Total ${total} records)`,
              }}
              scroll={{ x: 1200 }}
            />
          </TabPane>

          <TabPane tab="质量追溯 (Traceability)" key="traceability">
            <Card>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Alert
                  message="需求追踪 (Requirement Traceability)"
                  description="输入需求ID以追踪从需求到测试用例再到缺陷的关联关系"
                  type="info"
                  showIcon
                  icon={<ApartmentOutlined />}
                />

                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    placeholder="输入需求ID (Enter Requirement ID)"
                    value={requirementId}
                    onChange={(e) => setRequirementId(e.target.value)}
                    onPressEnter={handleTraceRequirement}
                  />
                  <Button
                    type="primary"
                    icon={<SearchOutlined />}
                    onClick={handleTraceRequirement}
                    loading={loading}
                  >
                    追踪 (Trace)
                  </Button>
                </Space.Compact>

                {traceabilityData && (
                  <Card
                    title="追溯结果 (Traceability Results)"
                    style={{ marginTop: 16 }}
                  >
                    <Descriptions bordered column={1}>
                      <Descriptions.Item label="需求ID (Requirement ID)">
                        {traceabilityData.requirementId}
                      </Descriptions.Item>
                      <Descriptions.Item label="关联用例数 (Linked Test Cases)">
                        <Badge
                          count={traceabilityData.linkedTestCases?.length || 0}
                          showZero
                          style={{ backgroundColor: '#52c41a' }}
                        />
                      </Descriptions.Item>
                      <Descriptions.Item label="发现缺陷数 (Defects Found)">
                        <Badge
                          count={traceabilityData.defectsFound?.length || 0}
                          showZero
                          style={{ backgroundColor: '#ff4d4f' }}
                        />
                      </Descriptions.Item>
                      <Descriptions.Item label="覆盖率 (Coverage)">
                        <Progress
                          percent={traceabilityData.coverage || 0}
                          size="small"
                        />
                      </Descriptions.Item>
                    </Descriptions>
                  </Card>
                )}
              </Space>
            </Card>
          </TabPane>

          <TabPane tab="质量门禁 (Quality Gates)" key="gates">
            {qualityGates && (
              <Card>
                <Descriptions bordered column={2}>
                  <Descriptions.Item label="门禁状态 (Gate Status)" span={2}>
                    <Badge
                      status={qualityGates.passed ? 'success' : 'error'}
                      text={qualityGates.passed ? '通过 (Passed)' : '未通过 (Failed)'}
                    />
                  </Descriptions.Item>
                  {qualityGates.checks &&
                    Object.entries(qualityGates.checks).map(([key, value]) => (
                      <Descriptions.Item label={key} key={key}>
                        <Badge
                          status={value ? 'success' : 'error'}
                          text={value ? '✓' : '✗'}
                        />
                      </Descriptions.Item>
                    ))}
                </Descriptions>
              </Card>
            )}
          </TabPane>

          <TabPane tab="报告摘要 (Summary)" key="summary">
            <Card>
              <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>
                {report.summary}
              </pre>
            </Card>
          </TabPane>
        </Tabs>
      </Card>

      {/* HTML Report Modal */}
      <Modal
        title="HTML Report Preview"
        open={showHtmlModal}
        onCancel={() => setShowHtmlModal(false)}
        width="80%"
        footer={[
          <Button key="close" onClick={() => setShowHtmlModal(false)}>
            关闭 (Close)
          </Button>,
        ]}
      >
        <div
          dangerouslySetInnerHTML={{ __html: htmlReport }}
          style={{ maxHeight: '70vh', overflow: 'auto' }}
        />
      </Modal>
    </div>
  )
}

export default QualityReport

