## Description: <br>
A skill to break large tasks into smaller actionable steps, optimize execution for each step, and dynamically adapt based on progress. <br>

This skill is ready for commercial/non-commercial use. <br>

## Publisher: <br>
[scccmsd](https://clawhub.ai/user/scccmsd) <br>

### License/Terms of Use: <br>


## Use Case: <br>
Users and agents use this skill to break broad goals into prioritized sub-tasks, track status, and adapt the plan as work progresses. <br>

### Deployment Geography for Use: <br>
Global <br>

## Known Risks and Mitigations: <br>
Risk: Planning suggestions may be inappropriate for high-impact, regulated, financial, account, public-posting, or irreversible actions. <br>
Mitigation: Review each suggested step before allowing an agent to act in those contexts. <br>
Risk: The included helper stores tasks only in memory, so task state is lost when the process ends. <br>
Mitigation: Use it for transient planning or add reviewed persistence before relying on it for durable tracking. <br>


## Reference(s): <br>
- [ClawHub skill page](https://clawhub.ai/scccmsd/skills/todo-skill) <br>


## Skill Output: <br>
**Output Type(s):** [Text, Markdown, Code, Guidance] <br>
**Output Format:** [Markdown task lists, status updates, and optional Python code behavior] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [In-memory task state in the included Python helper; no persistence is described.] <br>

## Skill Version(s): <br>
1.0.0 (source: server release metadata) <br>

## Ethical Considerations: <br>
Users should evaluate whether this skill is appropriate for their environment, review any generated or modified files before relying on them, and apply their organization's safety, security, and compliance requirements before deployment. <br>
