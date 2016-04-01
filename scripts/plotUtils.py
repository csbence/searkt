import re


def cnv_ns_to_ms(ns):
    return ns / 1000000.0


class Results:
    def __init__(self, parsedJson):
        self.configuration = (parsedJson['experimentConfiguration']['algorithmName'],
                              parsedJson['experimentConfiguration']['domainName'])
        self.generatedNodes = parsedJson['generatedNodes']
        self.expandedNodes = parsedJson['expandedNodes']
        self.actions = parsedJson['actions']
        self.time = cnv_ns_to_ms(parsedJson['goalAchievementTime'])


def translate_algorithm_name(alg_name):
    # Handle hat (^) names
    if "HAT" in alg_name:
        alg_name = re.sub(r"(.*)_(.*)_(HAT)", r"\1", alg_name) \
                   + re.sub(r"(.*)_(.*)_(HAT)", r"_$\\hat{\2}$", alg_name).lower()
    # Specific word formatting
    alg_name = alg_name.replace('DYNAMIC', 'Dynamic')
    alg_name = alg_name.replace('WEIGHTED', 'Weighted')
    alg_name = alg_name.replace('LSS_', 'LSS-')
    # Handle star (*) names
    alg_name = alg_name.replace('_STAR', '*')
    # Replace rest of underscores
    alg_name = alg_name.replace('_', ' ')
    return alg_name


def translate_domain_name(domain_name):
    # Replace underscores
    domain_name = domain_name.replace('_', ' ')
    # Convert case
    domain_name = domain_name.title()
    return domain_name
