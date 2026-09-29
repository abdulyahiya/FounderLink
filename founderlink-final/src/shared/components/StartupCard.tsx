import React from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin, TrendingUp, DollarSign, ArrowUpRight } from 'lucide-react';
import useAuth from '../hooks/useAuth';
import type { Startup } from '../../types';

interface StartupCardProps {
  startup: Startup;
}

const stageColors: Record<string, string> = {
  IDEA: 'badge-yellow',
  MVP: 'badge-blue',
  EARLY_TRACTION: 'badge-green',
  SCALING: 'badge-green',
};

const StartupCard: React.FC<StartupCardProps> = ({ startup }) => {
  const navigate = useNavigate();
  const { isInvestor, isCoFounder } = useAuth();

  const handleClick = () => {
    if (isInvestor) navigate(`/investor/startups/${startup.id}`);
    else if (isCoFounder) navigate(`/cofounder/startups/${startup.id}`);
    else navigate(`/founder/startups/${startup.id}/edit`);
  };

  return (
    <div className="card-hover group" onClick={handleClick}>
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0 mr-3">
          <h3 className="font-semibold text-white text-base truncate group-hover:text-accent-light transition-colors">
            {startup.name}
          </h3>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <span className={stageColors[startup.stage] || 'badge-blue'}>
            {startup.stage === 'EARLY_TRACTION' ? 'Early Traction' : startup.stage}
          </span>
          <ArrowUpRight size={14} className="text-gray-600 group-hover:text-accent-light transition-colors" />
        </div>
      </div>

      <p className="text-gray-400 text-sm line-clamp-2 mb-4 leading-relaxed">{startup.description}</p>

      <div className="flex items-center gap-4 text-xs text-gray-500 border-t border-dark-500 pt-3">
        <span className="flex items-center gap-1.5">
          <TrendingUp size={13} className="text-gray-600" />
          {startup.industry}
        </span>
        {startup.location && (
          <span className="flex items-center gap-1.5">
            <MapPin size={13} className="text-gray-600" />
            {startup.location}
          </span>
        )}
        <span className="flex items-center gap-1.5 ml-auto font-medium text-gray-300">
          <DollarSign size={13} className="text-green-500" />
          {Number(startup.fundingGoal).toLocaleString()}
        </span>
      </div>
    </div>
  );
};

export default StartupCard;
